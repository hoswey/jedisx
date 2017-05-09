package com.yy.jedis.sentinel;

import com.yy.jedis.JedisServer;
import com.yy.jedis.selector.CompositeSelector;
import com.yy.jedis.selector.LatencyMinimizingServerSelector;
import com.yy.jedis.selector.ServerMonitor;
import com.yy.jedis.selector.ServerSelector;
import com.yy.jedis.selector.SlavePreferredSelector;
import com.yy.jedis.sentinel.SentinelServer.SentinelEventListener;
import com.yy.jedis.utils.Assertions;
import com.yy.jedis.utils.CollectionUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Protocol;
import redis.clients.util.Pool;

import static com.yy.jedis.utils.Assertions.isTrue;

/**
 * @author hoswey
 */
@Slf4j
public class JedisxSentinelPool {

  private static final int DEFAULT_ACCEPTANCE_LATENCY = 5;

  private GenericObjectPoolConfig poolConfig;
  private int connectionTimeout;
  private int soTimeout;
  private String password;
  private int database;
  private String clientName;
  private String masterName;
  private JedisSentinelPool masterJedisSentinelPool;
  private Set<String> sentinels;

  private JedisServer masterServer;
  private List<JedisServer> slaveServers = new ArrayList<>();

  private List<JedisServer> jedisServers = new ArrayList<>();
  private List<HostAndPort> allHostAndPorts = new ArrayList<>();

  private SentinelServer sentinelServer;

  private ServerMonitor serverMonitor;

  private ServerSelector nearestSlavePreferredSelector =
      new CompositeSelector(
          Arrays.asList(new LatencyMinimizingServerSelector(DEFAULT_ACCEPTANCE_LATENCY,
              TimeUnit.MILLISECONDS), new SlavePreferredSelector()));

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
    this.clientName = clientName;
    this.sentinels = sentinels;

    initSentinelSever();
    initMasterPool();
    initSlavePools();
    startMonitorThread();
  }

  private void initSentinelSever() {

    sentinelServer = new SentinelServer(masterName, sentinels, new SentinelEventListener() {

      @Override
      public void onSlaveChange(List<JedisServer> newSlaveServers) {

        if (!CollectionUtils.isEqual(slaveServers, newSlaveServers)) {

          List<JedisServer> newJedisServers = new ArrayList<>();
          newJedisServers.add(masterServer);
          newJedisServers.addAll(slaveServers);

          for (JedisServer newJedisServer : newJedisServers) {
            initPool(newJedisServer);
          }

          serverMonitor.updateServers(newJedisServers);
          jedisServers = newJedisServers;

          for (JedisServer oldSlaveServer : slaveServers) {
            oldSlaveServer.stop();
          }
        }
      }

      @Override
      public void onMasterChange(JedisServer newMaster) {
        log.info("[cmd=onMasterChange,newMaster={}]", newMaster);
      }
    });
  }


  private void startMonitorThread() {
    serverMonitor = new ServerMonitor(this.jedisServers);
    serverMonitor.start();
  }

  private void initMasterPool() {

    this.masterJedisSentinelPool = new JedisSentinelPool(masterName, sentinels, poolConfig,
        connectionTimeout, soTimeout, password, database, clientName);

    JedisServer masterServer = sentinelServer.getMasterServer();
    masterServer.setPools(masterJedisSentinelPool);
    this.masterServer = masterServer;
    this.jedisServers.add(masterServer);
  }


  private void initSlavePools() {

    for (JedisServer jedisServer : sentinelServer.getSlavesSever()) {

      initPool(jedisServer);
      this.slaveServers.add(jedisServer);
      this.jedisServers.add(jedisServer);
    }
  }

  private void initPool(JedisServer jedisServer) {

    Assertions.isTrue("The pool has been already init", jedisServer.getPools() == null);

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
    for (JedisServer jedisServer : jedisServers) {
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
    return this.masterJedisSentinelPool.getResource();
  }

  /**
   * 获取最近的一个Slave Server, 假如没Slave, 则返回Master
   * 对一致性要求高的不能用该方法
   *
   * @return 最近的Jedis
   */
  public Jedis getNearestResource() {

    List<JedisServer> jedisServers = nearestSlavePreferredSelector.select(this.jedisServers);
    isTrue("no sever selected " + jedisServers, !jedisServers.isEmpty());

    return jedisServers.get(ThreadLocalRandom.current().nextInt(jedisServers.size())).getPools()
        .getResource();
  }
}
