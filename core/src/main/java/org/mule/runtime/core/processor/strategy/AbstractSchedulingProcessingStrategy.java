/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.util.rx.internal.ConditionalExecutorServiceDecorator;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class AbstractSchedulingProcessingStrategy implements ProcessingStrategy, Startable, Stoppable {

  public static final String TRANSACTIONAL_ERROR_MESSAGE = "Unable to process a transactional flow asynchronously";

  private Consumer<Scheduler> schedulerStopper;
  private MuleContext muleContext;

  public AbstractSchedulingProcessingStrategy(Consumer<Scheduler> schedulerStopper, MuleContext muleContext) {
    this.schedulerStopper = schedulerStopper;
    this.muleContext = muleContext;
  }

  protected Consumer<Scheduler> getSchedulerStopper() {
    return this.schedulerStopper;
  }

  protected MuleContext getMuleContext() {
    return this.muleContext;
  }

  protected ExecutorService getExecutorService(Scheduler scheduler) {
    return new ConditionalExecutorServiceDecorator(scheduler, scheduleOverridePredicate());
  }

  /**
   * Provides a way override the scheduling of tasks based on a predicate.
   * 
   * @return preficate that determines if task should be scheduled or processed in the current thread.
   */
  protected Predicate<Scheduler> scheduleOverridePredicate() {
    return scheduler -> false;
  }

}
