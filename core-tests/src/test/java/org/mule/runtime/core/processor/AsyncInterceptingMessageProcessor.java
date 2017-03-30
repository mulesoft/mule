/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Processes {@link Event}'s asynchronously using a {@link Scheduler} to schedule asynchronous processing of the next
 * {@link Processor}. The next {@link Processor} is therefore be executed in a different thread. Unlike the implementation in Mule
 * 3.x this implementation ignores {@link Event} attributes to determine if the flow is synchronous or transactional and
 * introduces an async boundary regardless. Also this implementation no longer handles exceptions during async processing and
 * these are instrad propagted.
 */
public class AsyncInterceptingMessageProcessor extends AbstractInterceptingMessageProcessor {

  private Supplier<Scheduler> scheduler;

  public AsyncInterceptingMessageProcessor(Supplier<Scheduler> scheduler) {
    this.scheduler = scheduler;
  }

  @Override
  public Event process(Event event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher).publishOn(fromExecutorService(scheduler.get())).transform(applyNext());
  }

  @Override
  public ProcessingType getProcessingType() {
    return CPU_LITE;
  }

}
