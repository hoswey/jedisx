package com.yy.jedis;

import com.yy.jedis.sentinel.SentinelEventListener;
import com.yy.jedis.sentinel.SentinelServer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
    sentinelServer = new SentinelServer("secondmaster", sentinels,new SentinelEventListener(){
      @Override
      public void onSlaveChange(List<JedisServer> newSlaveServers) {

      }

      @Override
      public void onMasterChange(JedisServer newMasterServers) {

      }
    });
    sentinelServer.start();
  }


  @Test
  public void findSlavesSever() {

    sentinelServer.getMasterServer();

//    Set<String> sentinels = new HashSet<>(Arrays.asList("172.26.40.16:26379",
//        "172.26.40.16:26380", "172.26.40.16:26381"));
//
//    Jedis jedis = new Jedis("172.26.40.16", 26380);
//    jedis.subscribe(new JedisPubSub() {
//      @Override
//      public void onMessage(String channel, String message) {
//        System.out.println("channel = " + channel + ", message = " + message);
//      }
//    }, "+switch-master", "+sdown", "-sdown");

  }

}