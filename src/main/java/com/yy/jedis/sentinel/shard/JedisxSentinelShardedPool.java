package com.yy.jedis.sentinel.shard;

import com.yy.jedis.sentinel.JedisxSentinelPool;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import redis.clients.jedis.Jedis;

/**
 * @author hoswey
 */
public class  JedisxSentinelShardedPool {

  private ShardedJedisxSentinelPool shadedPools;

  public JedisxSentinelShardedPool(List<String> masterNames, Set<String> sentinels) {

    List<JedisxSentinelPool> sentinelPools = new ArrayList<>();
    List<SentinelShardInfo> sentinelShardInfos = new ArrayList<>();
    for (String masterName : masterNames) {
      JedisxSentinelPool jedisxSentinelPool = new JedisxSentinelPool(masterName, sentinels);
      sentinelPools.add(jedisxSentinelPool);
      sentinelShardInfos.add(new SentinelShardInfo(jedisxSentinelPool));
    }
    shadedPools = new ShardedJedisxSentinelPool(sentinelShardInfos);
  }

  public Jedis getResource() {
    return new ShardedPoolResource(shadedPools, false);
  }


  public Jedis getNearestResource() {
    return new ShardedPoolResource(shadedPools, true);
  }

  public class ShardedPoolResource extends Jedis {

    private ShardedJedisxSentinelPool shadedPools;

    private boolean nearest;

    public ShardedPoolResource(ShardedJedisxSentinelPool shadedPools, boolean nearest) {
      this.shadedPools = shadedPools;
      this.nearest = nearest;
    }

    @Override
    public String set(final String key, String value) {
      try (Jedis jedis = getShard(key)) {
        return jedis.set(key, value);
      }
    }

    @Override
    public String get(final String key) {
      try (Jedis jedis = getShard(key)) {
        return jedis.get(key);
      }
    }

    private Jedis getShard(String key) {

      JedisxSentinelPool pool = shadedPools.getShard(key);
      if (nearest) {
        return pool.getNearestResource();
      } else {
        return pool.getResource();
      }
    }

    public void close() {
      //Do nothing as the resource is always closed
    }
  }
}
