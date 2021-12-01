/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_PROFILING_SERVICE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.FLOW_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory.BLOCKING_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ReactorPublisherBuilder.buildFlux;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactId;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactType;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getLocation;
import static org.mule.runtime.core.internal.util.rx.ReactorTransactionUtils.isTxActiveByContext;
import static org.mule.runtime.core.internal.util.rx.ReactorTransactionUtils.popTxFromSubscriberContext;
import static org.mule.runtime.core.internal.util.rx.ReactorTransactionUtils.pushTxToSubscriberContext;
import static java.lang.System.currentTimeMillis;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.subscriberContext;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.profiling.CoreProfilingService;
import org.mule.runtime.core.internal.profiling.context.DefaultComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;

/**
 * Decorates a {@link ProcessingStrategy} so that processing takes place on the current thread in the event of a transaction being
 * active.
 *
 * @since 4.3.0
 */
public class TransactionAwareStreamEmitterProcessingStrategyDecorator extends ProcessingStrategyDecorator {

  private static final Consumer<CoreEvent> NULL_EVENT_CONSUMER = event -> {
  };

  @Inject
  private CoreProfilingService profilingService;

  @Inject
  private MuleContext muleContext;

  public TransactionAwareStreamEmitterProcessingStrategyDecorator(ProcessingStrategy delegate) {
    super(delegate);
    if (delegate instanceof ProcessingStrategyAdapter) {
      ProcessingStrategyAdapter adapter = (ProcessingStrategyAdapter) delegate;

      adapter.setOnEventConsumer(NULL_EVENT_CONSUMER);
      Function<ScheduledExecutorService, ScheduledExecutorService> delegateDecorator = adapter.getSchedulerDecorator();
      adapter.setSchedulerDecorator(scheduler -> new ConditionalExecutorServiceDecorator(delegateDecorator.apply(scheduler),
                                                                                         currentScheduler -> isTransactionActive()));
    }
  }

  @Override
  public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
    Sink delegateSink = delegate.createSink(flowConstruct, pipeline);
    Sink syncSink = new ReactorSinkProviderBasedSink(new DefaultCachedThreadReactorSinkProvider(flowConstruct, p -> from(p)
        .subscriberContext(popTxFromSubscriberContext())
        .transform(pipeline)
        .subscriberContext(pushTxToSubscriberContext("source")), NULL_EVENT_CONSUMER));
    return new TransactionalDelegateSink(syncSink, delegateSink);
  }

  @Override
  public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
    ComponentLocation location = getLocation(pipeline);
    String artifactId = getArtifactId(muleContext);
    String artifactType = getArtifactType(muleContext);

    Function<CoreEvent, ComponentProcessingStrategyProfilingEventContext> transfomer =
        coreEvent -> new DefaultComponentProcessingStrategyProfilingEventContext(coreEvent, getLocation(pipeline),
                                                                                 Thread.currentThread().getName(), artifactId,
                                                                                 artifactType, currentTimeMillis());

    return pub -> subscriberContext()
        .flatMapMany(ctx -> {
          if (isTxActiveByContext(ctx)) {
            // The profiling events related to the processing strategy scheduling are triggered independently of this being
            // a blocking processing strategy that does not involve a thread switch.
            return buildFlux(pub)
                .profileProcessingStrategyEvent(profilingService,
                                                getDataProducer(PS_SCHEDULING_FLOW_EXECUTION),
                                                transfomer)
                .profileProcessingStrategyEvent(profilingService,
                                                getDataProducer(
                                                                STARTING_FLOW_EXECUTION),
                                                transfomer)
                .transform(BLOCKING_PROCESSING_STRATEGY_INSTANCE.onPipeline(pipeline))
                .profileProcessingStrategyEvent(profilingService,
                                                getDataProducer(
                                                                FLOW_EXECUTED),
                                                transfomer)
                .build();
          } else {
            return from(pub).transform(delegate.onPipeline(pipeline));
          }
        });
  }

  private ProfilingDataProducer<ComponentProcessingStrategyProfilingEventContext, CoreEvent> getDataProducer(
                                                                                                             ProfilingEventType<ComponentProcessingStrategyProfilingEventContext> eventType) {
    return profilingService.getProfilingDataProducer(eventType);
  }

  @Override
  public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
    ComponentLocation location = getLocation(processor);
    String artifactId = muleContext.getConfiguration().getId();
    String artifactType = muleContext.getArtifactType().getAsString();

    Function<CoreEvent, ComponentProcessingStrategyProfilingEventContext> transfomer =
        new Function<CoreEvent, ComponentProcessingStrategyProfilingEventContext>() {

          @Override
          public ComponentProcessingStrategyProfilingEventContext apply(CoreEvent coreEvent) {
            return new DefaultComponentProcessingStrategyProfilingEventContext(coreEvent, getLocation(processor),
                                                                               Thread.currentThread().getName(), artifactId,
                                                                               artifactType, currentTimeMillis());
          }
        };

    return pub -> subscriberContext()
        .flatMapMany(ctx -> {
          if (isTxActiveByContext(ctx)) {
            // The profiling events related to the processing strategy scheduling are triggered independently of this being
            // a blocking processing strategy that does not involve a thread switch.
            return buildFlux(pub)
                .profileProcessingStrategyEvent(profilingService,
                                                getDataProducer(PS_SCHEDULING_OPERATION_EXECUTION),
                                                transfomer)
                .profileProcessingStrategyEvent(profilingService,
                                                getDataProducer(PS_STARTING_OPERATION_EXECUTION),
                                                transfomer)
                .transform(BLOCKING_PROCESSING_STRATEGY_INSTANCE.onProcessor(processor))
                .profileProcessingStrategyEvent(profilingService,
                                                getDataProducer(
                                                                PS_OPERATION_EXECUTED),
                                                transfomer)
                .profileProcessingStrategyEvent(profilingService,
                                                getDataProducer(
                                                                PS_FLOW_MESSAGE_PASSING),
                                                transfomer)
                .build();
          } else {
            return from(pub).transform(delegate.onProcessor(processor));
          }
        });
  }
}
