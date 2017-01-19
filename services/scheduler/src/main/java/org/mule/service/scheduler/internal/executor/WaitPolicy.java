/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal.executor;

import static java.lang.Long.MAX_VALUE;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.mule.runtime.core.api.scheduler.SchedulerBusyException;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A handler for unexecutable tasks that waits until the task can be submitted for execution or times out. Generously snipped from
 * the jsr166 repository at:
 * <a href="http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/ThreadPoolExecutor.java"></a>.
 */
public class WaitPolicy implements RejectedExecutionHandler {

  private final long time;
  private final TimeUnit timeUnit;

  /**
   * Constructs a <tt>WaitPolicy</tt> which waits (almost) forever.
   */
  public WaitPolicy() {
    // effectively waits forever
    this(MAX_VALUE, SECONDS);
  }

  /**
   * Constructs a <tt>WaitPolicy</tt> with timeout. A negative <code>time</code> value is interpreted as
   * <code>Long.MAX_VALUE</code>.
   */
  public WaitPolicy(long time, TimeUnit timeUnit) {
    super();
    this.time = (time < 0 ? MAX_VALUE : time);
    this.timeUnit = timeUnit;
  }

  @Override
  @SuppressWarnings("boxing")
  public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
    try {
      if (!e.getQueue().offer(r, time, timeUnit)) {
        throw new SchedulerBusyException(format("Scheduler did not accept within %1d %2s", time, timeUnit));
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new RejectedExecutionException(ie);
    }
  }


}
