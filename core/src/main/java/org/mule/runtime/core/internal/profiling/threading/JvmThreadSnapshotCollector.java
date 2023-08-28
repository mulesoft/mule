/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling.threading;

import static java.lang.Thread.currentThread;
import static java.lang.management.ManagementFactory.getThreadMXBean;

import org.mule.runtime.api.profiling.threading.ThreadSnapshot;
import org.mule.runtime.api.profiling.threading.ThreadSnapshotCollector;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class JvmThreadSnapshotCollector implements ThreadSnapshotCollector {

  private static final ThreadMXBean threadMXBean = getThreadMXBean();

  public JvmThreadSnapshotCollector() {}

  // TODO: Evaluate moving this to a better place
  static {
    threadMXBean.setThreadContentionMonitoringEnabled(true);
    threadMXBean.setThreadCpuTimeEnabled(true);
  }

  @Override
  public ThreadSnapshot getCurrentThreadSnapshot() {
    final long id = currentThread().getId();
    ThreadInfo threadInfo = threadMXBean.getThreadInfo(id);
    return DefaultThreadSnapshot.builder()
        .withBlockedTime(threadInfo.getBlockedTime())
        .withWaitedTime(threadInfo.getWaitedTime())
        .withCPUTime(threadMXBean.getThreadCpuTime(id))
        .build();
  }
}
