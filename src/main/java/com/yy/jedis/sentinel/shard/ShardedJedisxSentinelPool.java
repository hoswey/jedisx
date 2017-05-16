package com.yy.jedis.sentinel.shard;

import com.yy.jedis.sentinel.JedisxSentinelPool;
import java.util.List;
import redis.clients.util.Sharded;

/**
 * @author hoswey
 */
class ShardedJedisxSentinelPool extends Sharded<JedisxSentinelPool, SentinelShardInfo> {

  public ShardedJedisxSentinelPool(List<SentinelShardInfo> shardInfos) {
    super(shardInfos);
  }
}
