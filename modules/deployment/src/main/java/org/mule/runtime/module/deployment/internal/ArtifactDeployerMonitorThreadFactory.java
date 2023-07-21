/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ArtifactDeployerMonitorThreadFactory implements ThreadFactory {

  static final AtomicInteger poolNumber = new AtomicInteger(1);
  final ThreadGroup group;
  final AtomicInteger threadNumber = new AtomicInteger(1);
  final String namePrefix;

  public ArtifactDeployerMonitorThreadFactory() {
    SecurityManager s = System.getSecurityManager();
    group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
    namePrefix = String.format("Mule.app.deployer.monitor.%d.thread.", poolNumber.getAndIncrement());
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
    // make sure it's non-daemon, allows for an 'idle' state of Mule by preventing early termination
    t.setDaemon(false);
    t.setPriority(Thread.MIN_PRIORITY);
    t.setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
    return t;
  }


}
