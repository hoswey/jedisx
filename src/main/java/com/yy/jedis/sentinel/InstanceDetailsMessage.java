package com.yy.jedis.sentinel;

import com.yy.jedis.ServerRole;
import lombok.Getter;
import lombok.Setter;

/**
 * @author hoswey
 */
@Getter
@Setter
public class InstanceDetailsMessage extends SentinelMessage {

  //<instance-type> <name> <ip> <port> @ <master-name> <master-ip> <master-port>
  private ServerRole instanceType;

  private String name;

  private String ip;

  private int port;

  private String masterName;

  private String masterIp;

  private int masterPort;

  public InstanceDetailsMessage(Type type) {
    super(type);
  }
}
