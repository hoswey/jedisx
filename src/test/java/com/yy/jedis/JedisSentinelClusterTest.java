package com.yy.jedis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * @author hoswey
 */
public class JedisSentinelClusterTest {

  @Test
  public void getResource() throws Exception {
  }

  @Test
  public void getNearestResource() throws Exception {

    Set<String> sentinels = new HashSet<>(Arrays.asList("172.26.40.16:26379",
        "172.26.40.16:26380", "172.26.40.16:26381"));
    JedisxSentinelPool cluster = new JedisxSentinelPool("mymaster", sentinels);

    TimeUnit.MINUTES.sleep(10);
  }

}