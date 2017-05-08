package com.yy.jedis.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author hoswey
 */
public class DaemonThreadFactory implements ThreadFactory {

  static final String nameSuffix = "]";
  final AtomicInteger threadNumber = new AtomicInteger(1);
  final String namePrefix;
  private ThreadGroup group;

  public DaemonThreadFactory(String poolName) {
    SecurityManager s = System.getSecurityManager();
    group = (s != null) ? s.getThreadGroup() :
        Thread.currentThread().getThreadGroup();
    namePrefix = poolName + " [Thread-";
  }


  public Thread newThread(Runnable r) {
    Thread t = new Thread(group,
        r,
        namePrefix +
            threadNumber.getAndIncrement() +
            nameSuffix,
        0);
    t.setDaemon(true);
    if (t.getPriority() != Thread.NORM_PRIORITY) {
      t.setPriority(Thread.NORM_PRIORITY);
    }
    return t;
  }
}