package com.yy.jedis.selector;

import com.yy.jedis.JedisServer;
import com.yy.jedis.concurrent.DaemonThreadFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

/**
 * @author hoswey
 */
@Slf4j
public class ServerMonitor {

  private ConcurrentHashMap<JedisServer, ScheduledFuture<?>> monitors;

  private ScheduledExecutorService scheduledExecutorService;

  public ServerMonitor() {

    this.scheduledExecutorService = Executors
        .newSingleThreadScheduledExecutor(new DaemonThreadFactory("Round Trip Time Monitor Pool"));
    this.monitors = new ConcurrentHashMap<>();
  }


  public void stop() {
    scheduledExecutorService.shutdownNow();
  }

  private void createMonitors(List<JedisServer> newServers) {
    for (JedisServer newServer : newServers) {
      createMonitor(newServer);
    }
  }

  private void createMonitor(JedisServer jedisServer) {

    ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
        new ServerMonitorRunnable(jedisServer),
        0,
        5,
        TimeUnit.SECONDS
    );

    log.info("[cmd=createMonitor,server={}]", jedisServer);
    monitors.put(jedisServer, scheduledFuture);
  }

  public void updateServers(List<JedisServer> newServers) {
    updateServers(new ArrayList<JedisServer>(), newServers);
  }

  public synchronized void updateServers(List<JedisServer> oldServers,
      List<JedisServer> newServers) {
    removeMonitors();
    createMonitors(newServers);
  }

  private void removeMonitors() {

    Iterator<Entry<JedisServer, ScheduledFuture<?>>> iterator = monitors.entrySet().iterator();
    while (iterator.hasNext()) {
      Entry<JedisServer, ScheduledFuture<?>> entry = iterator.next();
      boolean cancelled = entry.getValue().cancel(true);
      log.debug("[cmd=removeMonitors,server={},cancelled={}]", entry.getKey(), cancelled);
      iterator.remove();
    }
  }

  class ServerMonitorRunnable implements Runnable {

    private final ExponentiallyWeightedMovingAverage averageRoundTripTime = new ExponentiallyWeightedMovingAverage(
        0.2);

    @Getter
    private JedisServer jedisServer;

    public ServerMonitorRunnable(JedisServer jedisServer) {
      this.jedisServer = jedisServer;
    }

    @Override
    public void run() {

      long start = System.nanoTime();

      try (Jedis jedis = jedisServer.getPools().getResource()) {
        jedis.ping();
      } catch (RuntimeException re) {
        log.warn(
            "unable to connect to jedis server " + jedisServer.getHostAndPort() + " " + jedisServer
                .getName(), re);
      }

      long elapsedTimeNanos = System.nanoTime() - start;

      averageRoundTripTime.addSample(elapsedTimeNanos);
      jedisServer.setRoundTripTimeNanos(averageRoundTripTime.getAverage());

      log.debug(
          "[cmd=pingAndCalRoundTrip,HostAndPort={},elapsedTimeMills={},averageMills={},name={}]",
          jedisServer.getHostAndPort(),
          TimeUnit.MILLISECONDS.convert(elapsedTimeNanos, TimeUnit.NANOSECONDS),
          TimeUnit.MILLISECONDS.convert(averageRoundTripTime.getAverage(), TimeUnit.NANOSECONDS),
          jedisServer.getName()
      );
    }
  }
}
