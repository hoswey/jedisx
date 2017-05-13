package com.yy.jedis;

import com.yy.jedis.sentinel.JedisxSentinelPool;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import redis.clients.jedis.Jedis;

/**
 * @author hoswey
 */
@Slf4j
public class JedisxSentinelClusterTest {

  @Test
  public void getResource() throws Exception {
  }

  @Test
  public void getNearestResource() throws Exception {

    Set<String> sentinels = new HashSet<>(Arrays.asList("172.26.40.16:26379",
        "172.26.40.16:26380", "172.26.40.16:26381"));
    JedisxSentinelPool cluster = new JedisxSentinelPool("mymaster", sentinels);

    while (true) {

      try {
        String randomKey = ThreadLocalRandom.current().nextInt(0, 10000) + "";
        String randomValue = ThreadLocalRandom.current().nextInt(0, 10000) + "";
        try (Jedis jedis = cluster.getResource()) {
          jedis.set(randomKey, randomValue);
        }

        try (Jedis jedis = cluster.getNearestResource()) {
          String actualValue = jedis.get(randomKey);

          log.info("Expected Value {}, actual value {}, same {}", randomValue, actualValue,
              randomValue.equals(actualValue));
        }

        TimeUnit.SECONDS.sleep(5);
      } catch (Exception e) {

        log.error("", e);
        TimeUnit.SECONDS.sleep(5);}
    }
  }
}