package com.yy.jedis;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

/**
 * @author hoswey
 */
@Getter
@Setter
@EqualsAndHashCode(of = "hostAndPort")
@Builder
public class JedisServer {

  private long lastUpdateTimeNanos;

  private HostAndPort hostAndPort;

  private ServerRole serverType;

  private long roundTripTimeNanos;

  private Pool<Jedis> pools;

}
