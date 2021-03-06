package com.yy.jedis;

import com.yy.jedis.sentinel.JedisxSentinelPool;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * @author hoswey
 */
@Slf4j
public class JedisxSentinelClusterTest {

  @Test
  public void getResource() throws Exception {

    Set<String> sentinels = new HashSet<>(Arrays.asList("172.26.40.16:26379",
        "172.26.40.16:26380", "172.26.40.16:26381"));
    JedisxSentinelPool cluster = new JedisxSentinelPool("mymaster", sentinels);
    cluster.getResource().zadd("foo", System.currentTimeMillis(), "bar");
  }

  @Test
  public void getNearestResource() throws Exception {

//    Set<String> sentinels = new HashSet<>(Arrays.asList("172.26.40.16:26379",
//        "172.26.40.16:26380", "172.26.40.16:26381"));
//    JedisxSentinelPool cluster = new JedisxSentinelPool("mymaster", sentinels);
//
//    String randomKey = ThreadLocalRandom.current().nextInt(0, 10000) + "";
//    String randomValue = ThreadLocalRandom.current().nextInt(0, 10000) + "";
//    try (Jedis jedis = cluster.getResource()) {
//      jedis.set(randomKey, randomValue);
//    }
//
//    try (Jedis jedis = cluster.getNearestResource()) {
//      String actualValue = jedis.get(randomKey);
//
//      log.info("Expected Value {}, actual value {}, same {}", randomValue, actualValue,
//          randomValue.equals(actualValue));
//    }
  }
}