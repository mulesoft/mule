/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
