/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal.operations;

import static java.lang.management.ManagementFactory.getThreadMXBean;

import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperation;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationCallback;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationDefinition;
import org.mule.runtime.module.troubleshooting.internal.DefaultTroubleshootingOperationDefinition;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * Operation used to collect thread dumps from the JVM.
 * <p>
 * The name of the operation is "threadDump".
 */
public class ThreadDumpOperation implements TroubleshootingOperation {

  public static final String THREAD_DUMP_OPERATION_NAME = "threadDump";
  public static final String THREAD_DUMP_OPERATION_DESCRIPTION = "Collects a thread dump from the JVM";

  private static final TroubleshootingOperationDefinition definition = createOperationDefinition();

  @Override
  public TroubleshootingOperationDefinition getDefinition() {
    return definition;
  }

  @Override
  public TroubleshootingOperationCallback getCallback() {
    return (arguments, writer) -> {
      ThreadMXBean threadMXBean = getThreadMXBean();
      ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);

      for (ThreadInfo threadInfo : threadInfos) {
        writer.write(threadInfo.toString());
      }
    };
  }

  private static TroubleshootingOperationDefinition createOperationDefinition() {
    return new DefaultTroubleshootingOperationDefinition(THREAD_DUMP_OPERATION_NAME, THREAD_DUMP_OPERATION_DESCRIPTION);
  }
}
