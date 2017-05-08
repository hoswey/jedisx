package com.yy.jedis;

import com.yy.jedis.sentinel.JedisxSentinelPool;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Test;

/**
 * @author hoswey
 */
public class JedisxSentinelClusterTest {

  @Test
  public void getResource() throws Exception {
  }

  @Test
  public void getNearestResource() throws Exception {

    Set<String> sentinels = new HashSet<>(Arrays.asList("172.26.40.16:26379",
        "172.26.40.16:26380", "172.26.40.16:26381"));
    JedisxSentinelPool cluster = new JedisxSentinelPool("mymaster", sentinels);

    cluster.getNearestResource().get(ThreadLocalRandom.current().nextInt(0, 10000) + "");

    //TimeUnit.MINUTES.sleep(10);
  }

}