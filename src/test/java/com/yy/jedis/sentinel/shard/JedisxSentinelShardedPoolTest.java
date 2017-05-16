package com.yy.jedis.sentinel.shard;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author hoswey
 */
public class JedisxSentinelShardedPoolTest {

  private JedisxSentinelShardedPool pool;

  @Before
  public void setup() {

    Set<String> sentinels = new HashSet<>(Arrays.asList("172.26.40.16:26379",
        "172.26.40.16:26380", "172.26.40.16:26381"));
    pool = new JedisxSentinelShardedPool(Arrays.asList("firstmaster", "secondmaster"), sentinels);
  }

  @Test
  public void testGetSet() {

    for (int i = 0; i < 100; i++) {
      String key = "Key" + ThreadLocalRandom.current().nextInt();
      String value = "Value" + ThreadLocalRandom.current().nextInt();
      pool.getResource().set(key, value);
      Assert.assertEquals(value, pool.getNearestResource().get(key));
    }
  }

}