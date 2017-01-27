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
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import reactor.core.publisher.WorkQueueProcessor;

public abstract class AbstractSchedulingProcessingStrategy extends AbstractProcessingStrategy implements Startable, Stoppable {

  private Consumer<Scheduler> schedulerStopper;
  private MuleContext muleContext;

  public AbstractSchedulingProcessingStrategy(Consumer<Scheduler> schedulerStopper, MuleContext muleContext) {
    this.schedulerStopper = schedulerStopper;
    this.muleContext = muleContext;
  }

  @Override
  public Sink createSink(FlowConstruct flowConstruct, Function<Publisher<Event>, Publisher<Event>> function) {
    WorkQueueProcessor<Event> processor = WorkQueueProcessor.share(false);
    return new ReactorSink(processor.connectSink(), flowConstruct, processor.transform(function).retry().subscribe(),
                           createOnEventConsumer());
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
