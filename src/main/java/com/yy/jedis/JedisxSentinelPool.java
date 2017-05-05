package com.yy.jedis;

import com.yy.jedis.selector.NearestJedisSelector;
import com.yy.jedis.selector.ServerMonitor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Pool;

/**
 * @author hoswey
 */
public class JedisxSentinelPool {

  private GenericObjectPoolConfig poolConfig;

  private int connectionTimeout;
  private int soTimeout;
  private String password;
  private int database;
  private String clientName;
  private String masterName;
  private JedisSentinelPool masterJedisSentinelPool;
  private Set<String> sentinels;


  private List<JedisServer> jedisServers = new ArrayList<>();

  private NearestJedisSelector nearestJedisSelector = new NearestJedisSelector();

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

    initMasterPool();
    initSlavePools();
    startMonitorThread();
  }

  private void startMonitorThread() {
    ServerMonitor serverMonitor = new ServerMonitor(this.jedisServers);
    serverMonitor.startMonitor();
  }

  private void initMasterPool() {
    this.masterJedisSentinelPool = new JedisSentinelPool(masterName, sentinels, poolConfig,
        connectionTimeout, soTimeout, password, database, clientName);
    try (Jedis jedis = masterJedisSentinelPool.getResource()) {
      JedisServer jedisServer = parseSentinelCommandResponse(
          sentinel(Protocol.SENTINEL_GET_MASTER_ADDR_BY_NAME).get(0));
      jedisServer.setPools(masterJedisSentinelPool);
      this.jedisServers.add(jedisServer);
    }
  }

  public List<Map<String, String>> sentinel(String command) {

    List<Map<String, String>> response = null;
    for (String sentinel : sentinels) {

      HostAndPort hostAndPort = HostAndPort.parseString(sentinel);
      try (Jedis jedis = new Jedis(hostAndPort.getHost(), hostAndPort.getPort())) {
        if (Protocol.SENTINEL_GET_MASTER_ADDR_BY_NAME.equals(command)) {
          List<String> respStr = jedis.sentinelGetMasterAddrByName(masterName);
          response = new ArrayList<>();
          Map<String, String> map = new HashMap<>();
          map.put("ip", respStr.get(0));
          map.put("port", respStr.get(1));
          map.put("flags", "master");
          response.add(map);
          break;
        } else if (Protocol.SENTINEL_SLAVES.equals(command)) {
          response = jedis.sentinelSlaves(masterName);
          break;
        }
      } catch (RuntimeException re) {
        continue;
      }
    }

    if (response == null) {
      throw new JedisException("cannot connect to sentinels " + sentinels);
    }

    return response;
  }

  private void initSlavePools() {

    try (Jedis jedis = masterJedisSentinelPool.getResource()) {

      List<Map<String, String>> response = sentinel(Protocol.SENTINEL_SLAVES);
      List<JedisServer> jedisServers = new ArrayList<>();
      for (Map<String, String> item : response) {
        jedisServers.add(parseSentinelCommandResponse(item));
      }

      for (JedisServer jedisServer : jedisServers) {

        Pool<Jedis> slavePool = new JedisPool(poolConfig,
            jedisServer.getHostAndPort().getHost(),
            jedisServer.getHostAndPort().getPort(), connectionTimeout, soTimeout,
            password,
            database, null, false, null, null, null);

        jedisServer.setPools(slavePool);
        this.jedisServers.add(jedisServer);
      }
    }
  }

  public Jedis getResource() {
    return this.masterJedisSentinelPool.getResource();
  }

  public Jedis getNearestResource() {

    List<JedisServer> jedisServers = nearestJedisSelector.select(this.jedisServers);
    JedisServer randomJedis = jedisServers
        .get(ThreadLocalRandom.current().nextInt(jedisServers.size()));
    return randomJedis.getPools().getResource();
  }

  private JedisServer parseSentinelCommandResponse(Map<String, String> description) {
    HostAndPort hostAndPort = new HostAndPort(description.get("ip"),
        Integer.parseInt(description.get("port")));
    ServerRole serverType = ServerRole.codeOf(description.get("flags"));

    return JedisServer.builder().hostAndPort(hostAndPort).serverType(serverType).build();
  }
}
