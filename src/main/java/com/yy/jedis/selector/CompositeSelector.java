package com.yy.jedis.selector;

import com.yy.jedis.JedisServer;
import java.util.ArrayList;
import java.util.List;

import static com.yy.jedis.utils.Assertions.notNull;

/**
 * A server selector that composes a list of server selectors, and selects the servers by iterating
 * through the list from onSererChange to finish, passing the result of the previous into the next, and
 * finally returning the result of the last one.
 *
 * @author hoswey
 */
public final class CompositeSelector implements ServerSelector {

  private final List<ServerSelector> serverSelectors;

  /**
   * Constructs a new instance.
   *
   * @param serverSelectors the list of composed server selectors
   */
  public CompositeSelector(final List<? extends ServerSelector> serverSelectors) {

    notNull("serverSelectors", serverSelectors);

    if (serverSelectors.isEmpty()) {
      throw new IllegalArgumentException("Server selectors can not be an empty list");
    }

    for (ServerSelector cur : serverSelectors) {
      if (cur == null) {
        throw new IllegalArgumentException(
            "Can not have a null server selector in the list of composed selectors");
      }
    }
    this.serverSelectors = new ArrayList<>();
    for (ServerSelector cur : serverSelectors) {
      if (cur instanceof CompositeSelector) {
        this.serverSelectors.addAll(((CompositeSelector) cur).serverSelectors);
      } else {
        this.serverSelectors.add(cur);
      }
    }
  }

  @Override
  public List<JedisServer> select(final List<JedisServer> jedisServers) {

    List<JedisServer> choices = jedisServers;
    for (ServerSelector cur : serverSelectors) {
      choices = cur.select(choices);
    }

    return choices;
  }

  @Override
  public String toString() {
    return "{"
        + "serverSelectors=" + serverSelectors
        + '}';
  }
}
