/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.Thread.currentThread;
import static java.lang.management.ManagementFactory.getThreadMXBean;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class OperationThreadSnapshotCollector {

  private final ThreadMXBean threadMXBean;

  public OperationThreadSnapshotCollector() {
    this(getThreadMXBean());
  }

  OperationThreadSnapshotCollector(ThreadMXBean threadMXBean) {
    this.threadMXBean = threadMXBean;
    this.threadMXBean.setThreadContentionMonitoringEnabled(true);
    this.threadMXBean.setThreadCpuTimeEnabled(true);
  }

  public OperationThreadSnapshot collect() {
    final long id = currentThread().getId();
    ThreadInfo threadInfo = threadMXBean.getThreadInfo(id);
    return OperationThreadSnapshot.builder()
        .withBlockedTime(threadInfo.getBlockedTime())
        .withWaitedTime(threadInfo.getWaitedTime())
        .withCPUTime(threadMXBean.getThreadCpuTime(id))
        .build();
  }
}
