/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal.operations;

import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperation;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationCallback;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationDefinition;
import org.mule.runtime.module.troubleshooting.internal.DefaultTroubleshootingOperationDefinition;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

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
      try {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName objectName = new ObjectName("com.sun.management:type=DiagnosticCommand");
        Object[] commandArgs = {new String[0]};
        String[] signature = new String[] {String[].class.getName()};

        String threadDump = (String) mbeanServer.invoke(objectName, "threadPrint", commandArgs, signature);
        writer.write(threadDump);
      } catch (Exception e) {
        throw new RuntimeException("Failed to get thread dump", e);
      }
    };
  }

  private static TroubleshootingOperationDefinition createOperationDefinition() {
    return new DefaultTroubleshootingOperationDefinition(THREAD_DUMP_OPERATION_NAME, THREAD_DUMP_OPERATION_DESCRIPTION);
  }
}
