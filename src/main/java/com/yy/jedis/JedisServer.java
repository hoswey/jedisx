package com.yy.jedis;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

/**
 * @author hoswey
 */
@Getter
@Setter
@EqualsAndHashCode(of = "hostAndPort")
@ToString
public class JedisServer {

  private long lastUpdateTimeNanos;

  private HostAndPort hostAndPort;

  private ServerRole role;

  private long roundTripTimeNanos;

  private Pool<Jedis> pools;

  public JedisServer() {
    super();
  }

  public JedisServer(ServerRole role, String host, int port) {
    this.role = role;
    this.hostAndPort = new HostAndPort(host, port);
  }

  public void stop() {
    if (pools != null) {
      pools.destroy();
    }
  }
}
