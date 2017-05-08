package com.yy.jedis.selector;

import com.yy.jedis.JedisServer;
import com.yy.jedis.ServerRole;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author hoswey
 */
public class SlavePreferredSelector implements ServerSelector {

  @Override
  public List<JedisServer> select(List<JedisServer> JedisServers) {

    if (JedisServers.isEmpty()) {
      return Collections.emptyList();
    }

    List<JedisServer> slaves = new ArrayList<>();
    for (JedisServer jedisServer : JedisServers) {
      if (jedisServer.getRole() == ServerRole.SLAVE) {
        slaves.add(jedisServer);
      }
    }

    if (slaves.isEmpty()) {
      slaves.add(JedisServers.get(0));
    }
    return slaves;
  }
}
