package com.yy.jedis.selector;

import com.yy.jedis.JedisServer;
import java.util.List;

/**
 * @author hoswey
 */
public interface JedisSelector {

  List<JedisServer> select(List<JedisServer> jedisDescriptions);
}
