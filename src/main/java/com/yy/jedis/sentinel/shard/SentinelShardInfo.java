package com.yy.jedis.sentinel.shard;

import com.yy.jedis.sentinel.JedisxSentinelPool;
import redis.clients.util.ShardInfo;
import redis.clients.util.Sharded;

/**
 * @author hoswey
 */
 class SentinelShardInfo extends ShardInfo<JedisxSentinelPool> {

  private JedisxSentinelPool jedisxSentinelPool;

  public SentinelShardInfo(JedisxSentinelPool jedisxSentinelPool) {
    super(Sharded.DEFAULT_WEIGHT);
    this.jedisxSentinelPool = jedisxSentinelPool;
  }

  @Override
  protected JedisxSentinelPool createResource() {
    return jedisxSentinelPool;
  }

  @Override
  public String getName() {
    return null;
  }
}
