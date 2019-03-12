/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.privileged.extension;

import static java.util.concurrent.TimeUnit.SECONDS;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

import javax.inject.Inject;

public class PrivilegedNonBlockingComponentExecutor
    implements CompletableComponentExecutor<OperationModel>, Startable, Stoppable {

  public static final String OUTPUT = "Super Special Non Blocking";

  @Inject
  private SchedulerService schedulerService;
  private Scheduler ioScheduler;

  @Override
  public void start() throws MuleException {
    ioScheduler = schedulerService.ioScheduler();
  }

  @Override
  public void stop() throws MuleException {
    ioScheduler.stop();
  }

  @Override
  public void execute(ExecutionContext<OperationModel> executionContext, ExecutorCallback callback) {
    ioScheduler.schedule(() -> callback.complete(OUTPUT), 2, SECONDS);
  }
}
