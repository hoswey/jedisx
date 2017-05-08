package com.yy.jedis.sentinel;

import com.yy.jedis.sentinel.SentinelMessage.Type;

/**
 * @author hoswey
 */
public class SentinelMessageParser {

  public SentinelMessage parse(String channelName, String rawMessage) {

    Type type = Type.codeOf(channelName);
    if (type == null) {
      throw new IllegalStateException("cannot find type fpr channnel " + channelName);
    }
//
//    switch (type){
//      case SLAVE_ODOWN:
//      case SLAVE_SDOWN:
//    }
    return null;
  }
}
