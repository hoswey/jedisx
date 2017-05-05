package com.yy.jedis;

import lombok.Getter;

/**
 * @author hoswey
 */
public enum ServerRole {

  MASTER("master"), SLAVE("slave");

  @Getter
  private String code;

  ServerRole(String code) {
    this.code = code;
  }

  public static ServerRole codeOf(String code) {

    ServerRole serverRole = null;
    for (ServerRole s: ServerRole.values()){
      if (s.getCode().equals(code)){
         serverRole = s;
         break;
      }
    }

    if (serverRole == null){
      throw new IllegalArgumentException("cannot not find ServerType for " + code);
    }
    return serverRole;
  }
}
