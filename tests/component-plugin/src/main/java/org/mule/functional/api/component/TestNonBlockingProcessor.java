/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static org.mule.runtime.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.flatMap;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.justPublishOn;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import org.reactivestreams.Publisher;

import javax.inject.Inject;

/**
 * Test async non-blocking {@link Processor} implementation that will return control to the Flow in a custom {@link Scheduler}
 * thread in the same way as, for example, a HTTP requester would.
 */
public class TestNonBlockingProcessor extends AbstractComponent
    implements Processor, Initialisable, Disposable {

  private static int MAX_THREADS = 8;

  @Inject
  private SchedulerService schedulerService;

  private LazyValue<Scheduler> customScheduler;

  /**
   * Force the proactor to change the thread.
   */
  @Override
  public ProcessingType getProcessingType() {
    return CPU_LITE_ASYNC;
  }

  @Override
  public CoreEvent process(final CoreEvent event) throws MuleException {
    return event;
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return flatMap(publisher, event -> {
      if (isTransactionActive()) {
        return publisher;
      } else {
        return justPublishOn(event, customScheduler.get());
      }
    }, this);
  }

  @Override
  public void initialise() throws InitialisationException {
    customScheduler = new LazyValue<>(() -> schedulerService.customScheduler(config()
        .withName("TestNonBlockingProcessor[" + getLocation().getLocation() + "]")
        .withWaitAllowed(true)
        .withMaxConcurrentTasks(MAX_THREADS)));
  }

  @Override
  public void dispose() {
    customScheduler.ifComputed(sch -> sch.stop());
  }
}
