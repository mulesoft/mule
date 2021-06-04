/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.management.execution.DefaultExecutionOrchestrator;
import org.mule.runtime.core.internal.management.execution.DefaultProcessingStrategyExecutionProfiler;
import org.mule.runtime.core.internal.management.execution.ExecutionOrchestrator;
import org.mule.runtime.core.internal.management.execution.ProcessingStrategyExecutionProfiler;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy.PROCESSOR_SCHEDULER_CONTEXT_KEY;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

/**
 * Builder for a transformer that involves creation of a reactor chain with a processing strategy applied. The processing strategy
 * involves decisions concerning two schedulers:
 * <p>
 * The {@link Scheduler} used to dispatch the event to the processor.
 * <p>
 * The {@link Scheduler} used to dispatch the event again to the flow once the the response from the processor was obtained.
 * <p>
 * For performance issues, not always a thread switch is needed.
 *
 * @since 4.4.0, 4.3.1
 */
public class ComponentProcessingStrategyTransformerBuilder {

  private final ReactiveProcessor processor;
  private int parallelism = 1;
  private ExecutionOrchestrator executionOrchestrator = DefaultExecutionOrchestrator.getInstance();
  private ProcessingStrategyExecutionProfiler processingStrategyExecutionProfiler =
      DefaultProcessingStrategyExecutionProfiler.getInstance();
  private Scheduler contextScheduler;

  public ComponentProcessingStrategyTransformerBuilder(ReactiveProcessor processor, Scheduler contextScheduler) {
    this.processor = processor;
    this.contextScheduler = contextScheduler;
  }

  /**
   * Factory method for the builder.
   *
   * @param processor        processor from with the processing strategy should be created.
   * @param contextScheduler the context scheduler for the processing of the component processor.
   * 
   * @return the message processor chain builder
   */
  public static ComponentProcessingStrategyTransformerBuilder buildProcessorChainFrom(ReactiveProcessor processor,
                                                                                      Scheduler contextScheduler) {
    return new ComponentProcessingStrategyTransformerBuilder(processor, contextScheduler);

  }

  public ComponentProcessingStrategyTransformerBuilder withParallelism(int parallelism) {
    this.parallelism = parallelism;
    return this;
  }

  public ComponentProcessingStrategyTransformerBuilder withExecutionOrchestrator(ExecutionOrchestrator executionOrchestrator) {
    this.executionOrchestrator = executionOrchestrator;
    return this;
  }

  public ComponentProcessingStrategyTransformerBuilder withExecutionProfiler(ProcessingStrategyExecutionProfiler processingStrategyExecutionProfiler) {
    this.processingStrategyExecutionProfiler = processingStrategyExecutionProfiler;
    return this;
  }

  private Mono<CoreEvent> doBuildFromMono(CoreEvent event) {
    return Mono.just(event)
        .doOnNext(e -> processingStrategyExecutionProfiler.profileBeforeDispatchingToProcessor(e))
        .publishOn(fromExecutorService(executionOrchestrator.getDispatcherScheduler()))
        .doOnNext(e -> processingStrategyExecutionProfiler.profileBeforeComponentProcessing(e))
        .transform(processor)
        .doOnNext(e -> processingStrategyExecutionProfiler.profileBeforeComponentProcessing(e))
        .publishOn(fromExecutorService(executionOrchestrator.getCallbackScheduler()))
        .doOnNext(e -> processingStrategyExecutionProfiler.profileAfterDispatchingToFlow(e))
        .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, executionOrchestrator.getContextScheduler()));
  }


  private Flux<CoreEvent> doBuildFromFlux(Publisher<CoreEvent> publisher) {
    return Flux.from(publisher)
        .doOnNext(e -> processingStrategyExecutionProfiler.profileBeforeDispatchingToProcessor(e))
        .publishOn(fromExecutorService(executionOrchestrator.getDispatcherScheduler()))
        .doOnNext(e -> processingStrategyExecutionProfiler.profileBeforeComponentProcessing(e))
        .transform(processor)
        .doOnNext(e -> processingStrategyExecutionProfiler.profileBeforeComponentProcessing(e))
        .publishOn(fromExecutorService(executionOrchestrator.getCallbackScheduler()))
        .doOnNext(e -> processingStrategyExecutionProfiler.profileAfterDispatchingToFlow(e))
        .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, executionOrchestrator.getContextScheduler()));
  }

  public ReactiveProcessor build() {
    if (parallelism == 1) {
      return publisher -> doBuildFromFlux(publisher);
    } else {
      return publisher -> Flux.from(publisher)
          .flatMap(event -> doBuildFromMono(event), parallelism);
    }
  }

}
