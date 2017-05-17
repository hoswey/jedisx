package com.yy.jedis.sentinel;

import com.yy.jedis.JedisServer;
import com.yy.jedis.selector.CompositeSelector;
import com.yy.jedis.selector.LatencyMinimizingServerSelector;
import com.yy.jedis.selector.ServerMonitor;
import com.yy.jedis.selector.ServerSelector;
import com.yy.jedis.selector.SlavePreferredSelector;
import com.yy.jedis.utils.Assertions;
import com.yy.jedis.utils.CollectionUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;
import redis.clients.util.Pool;

import static com.yy.jedis.utils.Assertions.isTrue;

/**
 * @author hoswey
 */
@Slf4j
public class JedisxSentinelPool {

  private GenericObjectPoolConfig poolConfig;
  private int connectionTimeout;
  private int soTimeout;
  private String password;
  private int database;
  private String masterName;
  private Set<String> sentinels;

  private volatile JedisServer masterServer;
  private volatile List<JedisServer> slaveServers = new ArrayList<>();

  private SentinelServer sentinelServer;

  private ServerMonitor serverMonitor;

  private ReadWriteLock serversRWLock = new ReentrantReadWriteLock();

  private ServerSelector nearestSlavePreferredSelector =
      new CompositeSelector(
          Arrays.asList(new LatencyMinimizingServerSelector(), new SlavePreferredSelector()));

  public JedisxSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig) {
    this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE);
  }

  public JedisxSentinelPool(String masterName, Set<String> sentinels) {
    this(masterName, sentinels, new GenericObjectPoolConfig(), Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE);
  }

  public JedisxSentinelPool(String masterName, Set<String> sentinels, String password) {
    this(masterName, sentinels, new GenericObjectPoolConfig(), Protocol.DEFAULT_TIMEOUT, password);
  }

  public JedisxSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, int timeout, final String password) {
    this(masterName, sentinels, poolConfig, timeout, password, Protocol.DEFAULT_DATABASE);
  }

  public JedisxSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, final int timeout) {
    this(masterName, sentinels, poolConfig, timeout, null, Protocol.DEFAULT_DATABASE);
  }

  public JedisxSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, final String password) {
    this(masterName, sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, password);
  }

  public JedisxSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, int timeout, final String password,
      final int database) {
    this(masterName, sentinels, poolConfig, timeout, timeout, password, database);
  }

  public JedisxSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, int timeout, final String password,
      final int database, final String clientName) {
    this(masterName, sentinels, poolConfig, timeout, timeout, password, database, clientName);
  }

  public JedisxSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, final int timeout, final int soTimeout,
      final String password, final int database) {
    this(masterName, sentinels, poolConfig, timeout, soTimeout, password, database, null);
  }

  public JedisxSentinelPool(String masterName, Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, final int connectionTimeout, final int soTimeout,
      final String password, final int database, final String clientName) {
    this.masterName = masterName;
    this.poolConfig = poolConfig;
    this.connectionTimeout = connectionTimeout;
    this.soTimeout = soTimeout;
    this.password = password;
    this.database = database;
    this.sentinels = sentinels;

    initSentinelSever();
    startMonitorThread();
    initMasterPool();
    initSlavePools();
  }

  private void initSentinelSever() {

    sentinelServer = new SentinelServer(masterName, sentinels, new SentinelEventListener() {

      @Override
      public synchronized void onSlaveChange(List<JedisServer> newSlaveServers) {
        doOnSlaveServerChange(newSlaveServers);
      }

      @Override
      public void onMasterChange(JedisServer newMasterServers) {

        log.info("[cmd=onMasterChange,newMaster={}]", newMasterServers);
        doOnMasterServerChange(newMasterServers);

      }
    });
    sentinelServer.start();
  }

  private void doOnMasterServerChange(JedisServer newMasterServer) {

    Lock writerLock = serversRWLock.writeLock();
    writerLock.lock();
    try {
      if (!newMasterServer.equals(masterServer)) {
        List<JedisServer> olds = new ArrayList<>();
        if (masterServer != null) {
          olds.add(masterServer);
          masterServer.stop();
        }
        initPool(newMasterServer);
        masterServer = newMasterServer;
        updateMonitor();
      }
    } finally {
      writerLock.unlock();
    }
  }

  private void doOnSlaveServerChange(List<JedisServer> newSlaveServers) {

    log.debug("[cmd=doOnSlaveServerChange,newSlaveServers={}]", newSlaveServers);
    Lock writerLock = serversRWLock.writeLock();
    writerLock.lock();
    try {

      boolean isSlaveServersChanged = !CollectionUtils.isEqual(this.slaveServers, newSlaveServers);
      log.debug("[cmd=doOnSlaveServerChange,isSlaveServersChanged={}]", isSlaveServersChanged);

      if (isSlaveServersChanged) {

        for (JedisServer newSlaveServer : newSlaveServers) {
          initPool(newSlaveServer);
        }
        for (JedisServer oldSlaveServer : slaveServers) {
          oldSlaveServer.stop();
        }
        this.slaveServers = newSlaveServers;
        updateMonitor();
      }
    } finally {
      writerLock.unlock();
    }
  }

  private void updateMonitor() {
    serverMonitor.updateServers(CollectionUtils.concat(
        Collections.singletonList(masterServer),
        slaveServers));
  }

  private void startMonitorThread() {
    serverMonitor = new ServerMonitor();
  }

  private void initMasterPool() {

    JedisServer masterServer = sentinelServer.getMasterServer();
    doOnMasterServerChange(masterServer);
  }


  private void initSlavePools() {

    List<JedisServer> slaveServers = sentinelServer.getSlavesSever();
    doOnSlaveServerChange(slaveServers);
  }

  private void initPool(JedisServer jedisServer) {

    Assertions.isTrue(
        "The pool has been already init " + jedisServer.getHostAndPort() + " " + jedisServer
            .getName()
        , jedisServer.getPools() == null);

    Pool<Jedis> pool = new JedisPool(poolConfig,
        jedisServer.getHostAndPort().getHost(),
        jedisServer.getHostAndPort().getPort(), connectionTimeout, soTimeout,
        password,
        database, null, false, null, null, null);
    jedisServer.setPools(pool);
  }

  public void stop() {

    log.info("[cmd=stop,msg=begin stop]");

    this.serverMonitor.stop();
    this.sentinelServer.stop();
    for (JedisServer jedisServer : getServers()) {
      jedisServer.stop();
    }

    log.info("[cmd=stop,msg=stop successful]");
  }


  /**
   * 获取Master的Jedis
   *
   * @return Jedis
   */
  public Jedis getResource() {
    return this.masterServer.getPools().getResource();
  }

  /**
   * 获取最近的一个Slave Server, 假如没Slave, 则返回Master
   * 对一致性要求高的不能用该方法
   *
   * @return 最近的Jedis
   */
  public Jedis getNearestResource() {

    List<JedisServer> jedisServers = nearestSlavePreferredSelector.select(getServers());
    isTrue("no sever selected " + jedisServers, !jedisServers.isEmpty());

    JedisServer selected = jedisServers
        .get(ThreadLocalRandom.current().nextInt(jedisServers.size()));

    return selected.getPools()
        .getResource();
  }

  private List<JedisServer> getServers() {

    List<JedisServer> servers = new ArrayList<>();
    servers.add(this.masterServer);

    if (CollectionUtils.isNotEmpty(slaveServers)) {
      servers.addAll(this.slaveServers);
    }
    return servers;
  }
}
