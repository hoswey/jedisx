package com.yy.jedis.sentinel;

import com.yy.jedis.JedisServer;
import com.yy.jedis.ServerRole;
import com.yy.jedis.concurrent.DaemonThreadFactory;
import com.yy.jedis.utils.CollectionUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

/**
 * @author hoswey
 */
@Slf4j
public class SentinelServer {

  private Set<HostAndPort> hostAndPorts;

  private String masterName;

  private List<JedisServer> slaveServers;

  private JedisServer masterServer;

  private List<SentinelEventListener> sentinelEventListeners = new ArrayList<>();

  private Lock slaveLock = new ReentrantLock();
  private Condition slaveInitializeCondition = slaveLock.newCondition();

  private Lock masterLock = new ReentrantLock();
  private Condition masterInitializeCondition = masterLock.newCondition();

  public SentinelServer(String masterName, Set<String> sentinels,
      SentinelEventListener eventListener) {

    this.masterName = masterName;

    hostAndPorts = new HashSet<>();
    for (String sentinel : sentinels) {
      hostAndPorts.add(HostAndPort.parseString(sentinel));
    }
    this.sentinelEventListeners.add(eventListener);

    startSubscribeSentinemChannels();

    ScheduledExecutorService scheduledExecutorService = Executors
        .newSingleThreadScheduledExecutor(
            new DaemonThreadFactory("Redis Server Pooling Status Pool"));
    scheduledExecutorService.scheduleAtFixedRate(new MasterSlavesPoolingTask(),
        0, 10, TimeUnit.SECONDS);
  }


  @SuppressWarnings({"squid:S2274"})
  public JedisServer getMasterServer() {

    if (masterServer == null) {
      masterLock.lock();
      try {
        try {
          if (!masterInitializeCondition.await(5, TimeUnit.SECONDS)
              || masterServer == null) {
            throw new JedisException("Cannot discover master server in 5 second");
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      } finally {
        masterLock.unlock();
      }
    }

    return masterServer;
  }

  private boolean isConnectionOpen(HostAndPort hostAndPort) {

    boolean succ = false;
    try (Jedis jedis = new Jedis(hostAndPort.getHost(), hostAndPort.getPort())) {
      jedis.ping();
      succ = true;
    } catch (JedisException je) {
      log.warn("Cannot connect to server " + hostAndPort);
    }
    return succ;
  }

  @SuppressWarnings({"squid:S2274"})
  public List<JedisServer> getSlavesSever() {

    if (slaveServers == null) {
      slaveLock.lock();
      try {
        try {
          if (!slaveInitializeCondition.await(5, TimeUnit.SECONDS)) {
            throw new JedisException("Cannot discover slaves server in 5 second");
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      } finally {
        slaveLock.unlock();
      }
    }
    return slaveServers;
  }

  private boolean isObjectiveDown(String flags) {

    return flags.contains("o_down");
  }

  private boolean isSubjectiveDown(String flags) {
    return flags.contains("s_down") || flags.contains("disconnected");
  }

  public void startSubscribeSentinemChannels() {
    for (HostAndPort hostAndPort : hostAndPorts) {
      SentinelSubscriber sentinelListener = new SentinelSubscriber(masterName, hostAndPort);
      sentinelListener.setDaemon(true);
      sentinelListener.start();
    }
  }

  private void poolLatestSlavesAndNotify() {

    List<JedisServer> newSlaveServers = new ArrayList<>();

    for (HostAndPort hostAndPort : hostAndPorts) {

      try (Jedis jedis = new Jedis(hostAndPort.getHost(), hostAndPort.getPort())) {

        List<Map<String, String>> respList = jedis.sentinelSlaves(masterName);

        for (Map<String, String> resp : respList) {

          String ip = resp.get("ip");
          int port = Integer.parseInt(resp.get("port"));
          String flags = resp.get("flags");

          if (isObjectiveDown(flags)
              || isSubjectiveDown(flags) && !isConnectionOpen(new HostAndPort(ip, port))) {
            continue;
          }

          newSlaveServers.add(new JedisServer(ServerRole.SLAVE, ip, port));
        }
        break;
      } catch (JedisException re) {
        log.warn("execute sentinel command fail " + hostAndPort + ",try next", re);
      }
    }

    if (newSlaveServers.isEmpty()) {
      log.warn("Cannot find slave servers");
    }
    SentinelServer.this.slaveLock.lock();
    try {

      if (!CollectionUtils.isEqual(slaveServers, newSlaveServers)) {
        SentinelServer.this.slaveServers = newSlaveServers;
        SentinelServer.this.slaveInitializeCondition.signal();


      }
    } finally {
      SentinelServer.this.slaveLock.unlock();
    }
  }

  private void pollLatestMasterAndNotify() {

    JedisServer newMasterServer = null;
    for (HostAndPort hostAndPort : hostAndPorts) {

      try (Jedis jedis = new Jedis(hostAndPort.getHost(), hostAndPort.getPort())) {
        List<String> respStr = jedis.sentinelGetMasterAddrByName(masterName);
        String ip = respStr.get(0);
        int port = Integer.parseInt(respStr.get(1));

        newMasterServer = new JedisServer(ServerRole.MASTER, ip, port);
        break;

      } catch (JedisException re) {
        log.warn("execute sentinel command fail " + hostAndPort + ", try next", re);
      }
    }

    if (newMasterServer == null) {
      log.warn("Cannot find master server " + hostAndPorts);
      return;
    }

    if (!isConnectionOpen(newMasterServer.getHostAndPort())) {
      log.warn("Cannot connect to master server " + newMasterServer.getHostAndPort());
      return;
    }

    masterLock.lock();
    try {

      if (!Objects.equals(masterServer, newMasterServer)) {

        SentinelServer.this.masterServer = newMasterServer;
        SentinelServer.this.masterInitializeCondition.signal();
        //Can use a dedicate thread to dispatch this event
        onMasterChange(newMasterServer);
      }
    } finally {
      masterLock.unlock();
    }
  }

  private void onMasterChange(JedisServer newMasterServer) {
    for (SentinelEventListener listener : sentinelEventListeners) {
      listener.onMasterChange(newMasterServer);
    }
  }

  private void onSlaveChange(List<JedisServer> newSlaveServers) {
    for (SentinelEventListener listener : sentinelEventListeners) {
      listener.onSlaveChange(newSlaveServers);
    }
  }

  public interface SentinelEventListener {

    void onSlaveChange(List<JedisServer> newSlaves);

    void onMasterChange(JedisServer newMaster);
  }

  protected class MasterSlavesPoolingTask implements Runnable {

    @Override
    public void run() {
      pollLatestMasterAndNotify();
      poolLatestSlavesAndNotify();
    }
  }

  protected class SentinelSubscriber extends Thread {

    protected String masterName;
    protected HostAndPort hostAndPort;
    protected long subscribeRetryWaitTimeMillis = 5000;
    protected AtomicBoolean running = new AtomicBoolean(false);

    private SentinelChannelPubSub sentinelChannelPubSub;

    public SentinelSubscriber(String masterName, HostAndPort hostAndPort) {

      super(String.format("SentinelListener-%s-[%s:%d]", masterName, hostAndPort.getHost(),
          hostAndPort.getPort()));
      this.masterName = masterName;
      this.hostAndPort = hostAndPort;

      sentinelChannelPubSub = new SentinelChannelPubSub();
    }


    @Override
    public void run() {

      running.set(true);

      while (running.get()) {

        Jedis j = null;
        try {
          j = new Jedis(hostAndPort.getHost(), hostAndPort.getPort());
          j.subscribe(sentinelChannelPubSub, "+switch-master", "+sdown", "-sdown");
          log.debug("After subscribe sentinel channel");
        } catch (JedisConnectionException e) {

          if (running.get()) {
            log.error("Lost connection to Sentinel at " + hostAndPort.getHost() + ":" + hostAndPort
                .getPort()
                + ". Sleeping " + subscribeRetryWaitTimeMillis + "ms and retrying.", e);
            try {
              Thread.sleep(subscribeRetryWaitTimeMillis);
            } catch (InterruptedException e1) {
              Thread.currentThread().interrupt();
              log.error("Sleep interrupted: ", e1);
            }
          }
        } finally {
          if (j != null) {
            try {
              j.close();
            } catch (RuntimeException re) {
              log.error("Caught exception while close jedis: ", re);
            }
          }
        }
      }
    }

    public void shutdown() {
      try {
        if (!running.compareAndSet(true, false)) {
          return;
        }
        log.debug("Shutting down listener on " + hostAndPort);
        sentinelChannelPubSub.unsubscribe();
      } catch (Exception e) {
        log.error("Caught exception while shutting down: ", e);
      }
    }
  }

  class SentinelChannelPubSub extends JedisPubSub {

    @Override
    public void onMessage(String channel, String message) {

      log.info("Sentinel message: " + message);

      String masterName;
      switch (channel) {
        case "+switch-master":
          String[] switchMasterMsg = message.split(" ");
          masterName = switchMasterMsg[0];
          break;
        case "+sdown":
        case "-sdown":
          String[] sdownMsg = message.split(" ");
          masterName = sdownMsg[5];
          break;
        default:
          throw new IllegalStateException("unknown channel found " + channel);
      }

      String currentMasterName = SentinelServer.this.masterName;
      if (!currentMasterName.equals(masterName)) {
        log.info("Ignore msg as the master name is not equal current one {}, message is {}",
            currentMasterName, message);
        return;
      }

      switch (channel) {
        case "+switch-master":
          pollLatestMasterAndNotify();
          poolLatestSlavesAndNotify();
          break;
        case "+sdown":
        case "-sdown":
          String[] sdownMsg = message.split(" ");
          String instanceType = sdownMsg[0];
          if ("slave".equals(instanceType)) {
            poolLatestSlavesAndNotify();
          } else {
            log.info("Uninteresting instance type, message is", message);
          }
          break;
        default:
          throw new IllegalStateException("invalid channel " + channel);
      }
    }
  }
}