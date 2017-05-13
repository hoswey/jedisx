package com.yy.jedis.sentinel;

import com.yy.jedis.JedisServer;
import java.util.List;

/**
 * Callback listener of server event base on the sentinel publish message
 *
 * @author hoswey
 * @see <a href="https://redis.io/topics/sentinel#pubsub-messages">Pub/Sub Messages</a>.
 */
public interface SentinelEventListener {

  void onSlaveChange(List<JedisServer> newSlaveServers);

  void onMasterChange(JedisServer newMasterServers);
}

