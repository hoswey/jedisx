package com.yy.jedis.selector;

import com.yy.jedis.JedisServer;
import org.junit.Test;

/**
 * @author hoswey
 */
public class ServerMonitorTest {

  @Test
  public void testUpdateServers() throws Exception{
//
//    ServerMonitor serverMonitor = new ServerMonitor();
//    JedisServer s6379 = new JedisServer(ServerRole.MASTER, "172.26.40.16", 6379);
//    JedisServer s6380 = new JedisServer(ServerRole.MASTER, "172.26.40.16", 6380);
//    initServer(s6379);
//    initServer(s6380);
//
//
//    serverMonitor.updateServers(null, Arrays.asList(s6379));
//    Thread.sleep(5000);
//
//
//    serverMonitor.updateServers(Arrays.asList(s6379), Arrays.asList(s6380));
//    s6379.stop();
//
//    Thread.sleep(500000);

  }

  private void initServer(JedisServer jedisServer) {

//    Pool<Jedis> pool = new JedisPool(
//        jedisServer.getHostAndPort().getHost(),
//        jedisServer.getHostAndPort().getPort());
//    jedisServer.setPools(pool);

  }

}