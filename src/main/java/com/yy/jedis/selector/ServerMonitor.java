package com.yy.jedis.selector;

import com.yy.jedis.JedisServer;
import java.util.List;
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

  private List<JedisServer> jedisServers;

  private ConcurrentHashMap<JedisServer, ScheduledFuture<?>> monitors;

  private ScheduledExecutorService scheduledExecutorService;

  public ServerMonitor(List<JedisServer> jedisServers) {

    this.jedisServers = jedisServers;
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    this.monitors = new ConcurrentHashMap<>();
  }

  public void start() {

    for (JedisServer jedisServer : jedisServers) {
      startMonitor(jedisServer);
    }
  }

  private void startMonitor(JedisServer jedisServer) {

    ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
        new ServerMonitorRunnable(jedisServer),
        0,
        5,
        TimeUnit.SECONDS
    );
    monitors.put(jedisServer, scheduledFuture);
  }

  public void updateServers(List<JedisServer> jedisServers) {
    this.jedisServers = jedisServers;
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

        long elapsedTimeNanos = System.nanoTime() - start;
        averageRoundTripTime.addSample(elapsedTimeNanos);
        jedisServer.setRoundTripTimeNanos(averageRoundTripTime.getAverage());

        log.debug("[cmd=pingAndCalRoundTrip,elapsedTimeMills={},average={}]",
            elapsedTimeNanos / 1000,
            averageRoundTripTime.getAverage() / 1000);

      } catch (RuntimeException re) {
        log.warn("unable to connect to jedis server " + jedisServer.getHostAndPort(), re);
      }
    }
  }
}
