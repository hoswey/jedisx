package com.yy.jedis.sentinel;

/**
 * @author hoswey
 */
public class SwitchMasterMessage extends SentinelMessage {

  //<master name> <oldip> <oldport> <newip> <newport>
  private String masterName;

  private String oldIp;

  private int oldPort;

  private String newIp;

  private int newPort;

  protected SwitchMasterMessage() {
    super(Type.PLUS_SWITCH_MASTER);
  }

}
