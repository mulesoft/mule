/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal.executor;

import static java.lang.Thread.currentThread;
import static java.util.Collections.unmodifiableSet;

import org.mule.runtime.core.api.scheduler.SchedulerBusyException;

import java.util.Set;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

/**
 * Dynamically determines the {@link RejectedExecutionHandler} implementation to use according to the {@link ThreadGroup} of the
 * current thread. If the current thread is not a {@link org.mule.runtime.core.api.scheduler.SchedulerService} managed thread then
 * {@link WaitPolicy} is used.
 * 
 * @see AbortPolicy
 * @see WaitPolicy
 * 
 * @since 4.0
 */
public final class ByCallerThreadGroupPolicy implements RejectedExecutionHandler {

  private final AbortPolicy abort = new AbortPolicy() {

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
      throw new SchedulerBusyException("Task " + r.toString() + " rejected from " + executor.toString());
    }
  };
  private final WaitPolicy wait = new WaitPolicy();

  private final Set<ThreadGroup> waitGroups;
  private final ThreadGroup parentGroup;

  /**
   * Builds a new {@link ByCallerThreadGroupPolicy} with the given {@code waitGroups}.
   * 
   * @param waitGroups the group of threads for which a {@link WaitPolicy} will be applied. For the rest, an {@link AbortPolicy}
   *        will be applied.
   * @param parentGroup the {@link org.mule.runtime.core.api.scheduler.SchedulerService } parent {@link ThreadGroup}
   */
  public ByCallerThreadGroupPolicy(Set<ThreadGroup> waitGroups, ThreadGroup parentGroup) {
    this.waitGroups = unmodifiableSet(waitGroups);
    this.parentGroup = parentGroup;
  }

  @Override
  public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    ThreadGroup currentThreadGroup = currentThread().getThreadGroup();
    if (!isSchedulerThread(currentThreadGroup) || waitGroups.contains(currentThreadGroup)) {
      // MULE-11460 Make CPU-intensive pool a ForkJoinPool - keep the parallelism when waiting.
      wait.rejectedExecution(r, executor);
    } else {
      abort.rejectedExecution(r, executor);
    }
  }

  private boolean isSchedulerThread(ThreadGroup threadGroup) {
    if (threadGroup != null) {
      while (threadGroup.getParent() != null) {
        if (threadGroup.equals(parentGroup)) {
          return true;
        } else {
          threadGroup = threadGroup.getParent();
        }
      }
    }
    return false;
  }


}
