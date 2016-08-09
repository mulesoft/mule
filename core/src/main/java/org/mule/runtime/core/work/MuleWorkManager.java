/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.work;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.work.WorkExecutor;
import org.mule.runtime.core.config.ImmutableThreadingProfile;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.resource.spi.XATerminator;
import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkCompletedException;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>MuleWorkManager</code> is a JCA Work manager implementation used to manage thread allocation for Mule components and
 * connectors. This code has been adapted from the Geronimo implementation.
 */
public class MuleWorkManager implements WorkManager, MuleContextAware {

  /**
   * logger used by this class
   */
  protected static final Logger logger = LoggerFactory.getLogger(MuleWorkManager.class);

  /**
   * Forced shutdown delay. The time the workManager will wait while threads are being interrupted. The graceful shutdown timeout
   * which is the amount of time that the workManager will wait while the workManager completed pending and currently executing
   * jobs is configured using {@link MuleConfiguration}.
   */
  private static final long FORCEFUL_SHUTDOWN_TIMEOUT = 5000L;

  /**
   * The ThreadingProfile used for creation of the underlying ExecutorService
   */
  private final ThreadingProfile threadingProfile;

  /**
   * The actual pool of threads used by this MuleWorkManager to process the Work instances submitted via the
   * (do,start,schedule)Work methods.
   */
  private volatile ExecutorService workExecutorService;
  private final String name;
  private int gracefulShutdownTimeout;
  private MuleContext muleContext;

  /**
   * Various policies used for work execution
   */
  private final WorkExecutor scheduleWorkExecutor = new ScheduleWorkExecutor();
  private final WorkExecutor startWorkExecutor = new StartWorkExecutor();
  private final WorkExecutor syncWorkExecutor = new SyncWorkExecutor();

  public MuleWorkManager(ThreadingProfile profile, String name, int shutdownTimeout) {
    super();

    if (name == null) {
      name = "WorkManager#" + hashCode();
    }

    this.threadingProfile = new ImmutableThreadingProfile(profile);
    this.name = name;
    gracefulShutdownTimeout = shutdownTimeout;
  }

  public synchronized void start() throws MuleException {
    gracefulShutdownTimeout = getMuleContext().getConfiguration().getShutdownTimeout();

    if (workExecutorService == null) {
      workExecutorService = threadingProfile.createPool(name);
    }
  }

  public synchronized void dispose() {
    if (workExecutorService != null) {
      // Disable new tasks from being submitted
      workExecutorService.shutdown();
      try {
        // Wait a while for existing tasks to terminate
        if (!workExecutorService.awaitTermination(gracefulShutdownTimeout, TimeUnit.MILLISECONDS)) {
          // Cancel currently executing tasks and return list of pending
          // tasks
          List outstanding = workExecutorService.shutdownNow();
          // Wait a while for tasks to respond to being cancelled
          if (!workExecutorService.awaitTermination(FORCEFUL_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS)) {
            logger.warn(MessageFormat.format("Pool {0} did not terminate in time; {1} work items were cancelled.", name,
                                             outstanding.isEmpty() ? "No" : Integer.toString(outstanding.size())));
          } else {
            if (!outstanding.isEmpty()) {
              logger.warn(MessageFormat.format("Pool {0} terminated; {1} work items were cancelled.", name,
                                               Integer.toString(outstanding.size())));
            }
          }
        }
      } catch (InterruptedException ie) {
        // (Re-)Cancel if current thread also interrupted
        workExecutorService.shutdownNow();
        // Preserve interrupt status
        Thread.currentThread().interrupt();
      } finally {
        workExecutorService = null;
      }
    }
  }


  // TODO
  public XATerminator getXATerminator() {
    return null;
  }

  public void doWork(Work work) throws WorkException {
    executeWork(new WorkerContext(work), syncWorkExecutor);
  }

  public void doWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener) throws WorkException {
    WorkerContext workWrapper = new WorkerContext(work, startTimeout, execContext, workListener);
    workWrapper.setThreadPriority(Thread.currentThread().getPriority());
    executeWork(workWrapper, syncWorkExecutor);
  }

  public long startWork(Work work) throws WorkException {
    WorkerContext workWrapper = new WorkerContext(work);
    workWrapper.setThreadPriority(Thread.currentThread().getPriority());
    executeWork(workWrapper, startWorkExecutor);
    return System.currentTimeMillis() - workWrapper.getAcceptedTime();
  }

  public long startWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener)
      throws WorkException {
    WorkerContext workWrapper = new WorkerContext(work, startTimeout, execContext, workListener);
    workWrapper.setThreadPriority(Thread.currentThread().getPriority());
    executeWork(workWrapper, startWorkExecutor);
    return System.currentTimeMillis() - workWrapper.getAcceptedTime();
  }

  public void scheduleWork(Work work) throws WorkException {
    WorkerContext workWrapper = new WorkerContext(work);
    workWrapper.setThreadPriority(Thread.currentThread().getPriority());
    executeWork(workWrapper, scheduleWorkExecutor);
  }

  public void scheduleWork(Work work, long startTimeout, ExecutionContext execContext, WorkListener workListener)
      throws WorkException {
    WorkerContext workWrapper = new WorkerContext(work, startTimeout, execContext, workListener);
    workWrapper.setThreadPriority(Thread.currentThread().getPriority());
    executeWork(workWrapper, scheduleWorkExecutor);
  }

  /**
   * @see Executor#execute(Runnable)
   */
  public void execute(Runnable work) {
    if (!isStarted()) {
      throw new IllegalStateException("This MuleWorkManager '" + name + "' is stopped");
    }
    workExecutorService.execute(work);
  }

  /**
   * Execute the specified Work.
   *
   * @param work Work to be executed.
   * @exception WorkException Indicates that the Work execution has been unsuccessful.
   */
  private void executeWork(WorkerContext work, WorkExecutor workExecutor) throws WorkException {
    if (!isStarted()) {
      throw new IllegalStateException("This MuleWorkManager '" + name + "' is stopped");
    }

    try {
      work.workAccepted(this);
      workExecutor.doExecute(work, workExecutorService);
      WorkException exception = work.getWorkException();
      if (null != exception) {
        throw exception;
      }
    } catch (InterruptedException e) {
      WorkCompletedException wcj = new WorkCompletedException("The execution has been interrupted for WorkManager: " + name, e);
      wcj.setErrorCode(WorkException.INTERNAL);
      throw wcj;
    }
  }

  public boolean isStarted() {
    return (workExecutorService != null && !workExecutorService.isShutdown());
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }

  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
    if (this.threadingProfile != null && muleContext != null) {
      threadingProfile.setMuleContext(muleContext);
    }
  }

  protected ThreadingProfile getThreadingProfile() {
    return threadingProfile;
  }
}
