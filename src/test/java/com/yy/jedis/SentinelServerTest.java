package com.yy.jedis;

import com.yy.jedis.sentinel.SentinelServer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * @author hoswey
 */
public class SentinelServerTest {

  private SentinelServer sentinelServer;

  @Before
  public void setup() {
    Set<String> sentinels = new HashSet<>(Arrays.asList("172.26.40.16:26379",
        "172.26.40.16:26380", "172.26.40.16:26381"));
    //sentinelServer = new SentinelServer("mymaster", sentinels);
  }

  @Test
  public void findSlavesSever()   {
    System.out.println(sentinelServer.getSlavesSever());
  }

}