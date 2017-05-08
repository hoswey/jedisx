package com.yy.jedis.selector;

import com.yy.jedis.JedisServer;
import java.util.List;

/**
 * @author hoswey
 */
public interface ServerSelector {

  List<JedisServer> select(List<JedisServer> JedisServers);
}
