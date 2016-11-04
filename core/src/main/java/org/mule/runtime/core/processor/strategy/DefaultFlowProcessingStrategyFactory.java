/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategyFactory.SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

/**
 * This factory's processing strategy uses the 'asynchronous' strategy where possible, but if an event is synchronous it processes
 * it synchronously rather than failing.
 */
public class DefaultFlowProcessingStrategyFactory extends LegacyAsynchronousProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext) {
    return new DefaultFlowProcessingStrategy(() -> {
      try {
        return muleContext.getRegistry().lookupObject(SchedulerService.class).ioScheduler();
      } catch (RegistrationException e) {
        throw new MuleRuntimeException(e);
      }
    }, scheduler -> scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(), MILLISECONDS), muleContext);
  }

  static class DefaultFlowProcessingStrategy extends LegacyAsynchronousProcessingStrategy {

    public DefaultFlowProcessingStrategy(Supplier<Scheduler> schedulerSupplier, Consumer<Scheduler> schedulerStopper,
                                         MuleContext muleContext) {
      super(schedulerSupplier, schedulerStopper, muleContext);
    }

    @Override
    public Function<Publisher<Event>, Publisher<Event>> onPipeline(Pipeline pipeline,
                                                                   Function<Publisher<Event>, Publisher<Event>> pipelineFunction) {
      return publisher -> from(publisher).concatMap(request -> {
        if (canProcessAsync(request)) {
          return just(request).transform(super.onPipeline(pipeline, pipelineFunction));
        } else {
          return just(request).transform(SYNCHRONOUS_PROCESSING_STRATEGY_INSTANCE.onPipeline(pipeline, pipelineFunction));
        }
      });
    }

    protected boolean canProcessAsync(Event event) {
      return !(event.isSynchronous() || event.isTransacted());
    }
  }
}
