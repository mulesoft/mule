/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler.internal.threads;

import static java.lang.String.format;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link ThreadFactory} implementation that sets a {@link ThreadGroup} and a name with a counter to the created {@link Thread}s
 *
 * @since 4.0
 */
public class SchedulerThreadFactory implements java.util.concurrent.ThreadFactory {

  private final ThreadGroup group;
  private final String nameFormat;
  private final AtomicLong counter;

  public SchedulerThreadFactory(ThreadGroup group) {
    this(group, "%s.%02d");
  }

  public SchedulerThreadFactory(ThreadGroup group, String nameFormat) {
    this.group = group;
    this.nameFormat = nameFormat;
    this.counter = new AtomicLong(1);
  }

  @Override
  public Thread newThread(Runnable runnable) {
    return new Thread(group, runnable, format(nameFormat, group.getName(), counter.getAndIncrement()));
  }

  public AtomicLong getCounter() {
    return counter;
  }
}
