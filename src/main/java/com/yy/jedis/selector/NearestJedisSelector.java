package com.yy.jedis.selector;

import com.yy.jedis.JedisServer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author hoswey
 */
public class NearestJedisSelector implements JedisSelector {

  @Override
  public List<JedisServer> select(List<JedisServer> jedisServers) {

    List<JedisServer> list = new ArrayList<>(jedisServers);
    Collections.sort(list,
        new Comparator<JedisServer>() {
          @Override
          public int compare(JedisServer o1, JedisServer o2) {
            return Long.valueOf(o1.getRoundTripTimeNanos()).compareTo(o2.getRoundTripTimeNanos());
          }
        });

    List<JedisServer> nearest = new ArrayList<>();
    nearest.add(list.get(0));
    return nearest;
  }
}
