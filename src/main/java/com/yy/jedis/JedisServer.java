package com.yy.jedis;

import java.util.UUID;
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
@ToString
@EqualsAndHashCode(of = "hostAndPort")
public class JedisServer {

  private long lastUpdateTimeNanos;

  private HostAndPort hostAndPort;

  private ServerRole role;

  private long roundTripTimeNanos;

  private String name = UUID.randomUUID().toString();

  private Pool<Jedis> pools = null;

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
