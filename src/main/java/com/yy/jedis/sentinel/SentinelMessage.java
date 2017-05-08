package com.yy.jedis.sentinel;

import lombok.Getter;

/**
 * @author hoswey
 * @link https://redis.io/topics/sentinel
 */
@Getter
public abstract class SentinelMessage {

  private String masterName;

  private Type type;

  protected SentinelMessage(Type type) {
    this.type = type;
  }

  public static enum Type {
    PLUS_SWITCH_MASTER("+switch-master"),
    SLAVE_SDOWN("+sdown"),
    SLAVE_ODOWN("+odown");

    @Getter
    private String code;

    Type(String code) {
      this.code = code;
    }

    public static Type codeOf(String code) {
      for (Type t : Type.values()) {
        if (t.code.equals(code)) {
          return t;
        }
      }
      return null;
    }
  }
}
