/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_PROFILING_SERVICE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.FLOW_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory.BLOCKING_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ReactorPublisherBuilder.buildFlux;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactId;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactType;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getLocation;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.subscriberContext;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.util.context.Context;

import javax.inject.Inject;

/**
 * Decorates a {@link ProcessingStrategy} so that processing takes place on the current thread in the event of a transaction being
 * active.
 *
 * @since 4.3.0
 */
public class TransactionAwareStreamEmitterProcessingStrategyDecorator extends ProcessingStrategyDecorator {

  private static final String TX_SCOPES_KEY = "mule.tx.activeTransactionsInReactorChain";

  private static final Consumer<CoreEvent> NULL_EVENT_CONSUMER = event -> {
  };

  @Inject
  private ProfilingService profilingService;

  @Inject
  private FeatureFlaggingService featureFlags;

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
    Sink syncSink = new StreamPerThreadSink(p -> from(p)
        .subscriberContext(popTxFromSubscriberContext())
        .transform(pipeline)
        .subscriberContext(pushTxToSubscriberContext("source")), NULL_EVENT_CONSUMER, flowConstruct);
    return new TransactionalDelegateSink(syncSink, delegateSink);
  }

  @Override
  public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
    ComponentLocation location = getLocation(pipeline);
    String artifactId = getArtifactId(muleContext);
    String artifactType = getArtifactType(muleContext);

    return pub -> subscriberContext()
        .flatMapMany(ctx -> {
          if (isTxActive(ctx)) {
            // The profiling events related to the processing strategy scheduling are triggered independently of this being
            // a blocking processing strategy that does not involve a thread switch.
            return buildFlux(pub)
                .profileEvent(location, getDataProducer(PS_SCHEDULING_FLOW_EXECUTION), artifactId, artifactType)
                .profileEvent(location, getDataProducer(STARTING_FLOW_EXECUTION), artifactId, artifactType)
                .transform(BLOCKING_PROCESSING_STRATEGY_INSTANCE.onPipeline(pipeline))
                .profileEvent(location, getDataProducer(FLOW_EXECUTED), artifactId, artifactType)
                .build();
          } else {
            return from(pub).transform(delegate.onPipeline(pipeline));
          }
        });
  }

  private Optional<ProfilingDataProducer<ProcessingStrategyProfilingEventContext>> getDataProducer(
                                                                                                   ProfilingEventType<ProcessingStrategyProfilingEventContext> eventType) {
    if (featureFlags.isEnabled(ENABLE_PROFILING_SERVICE)) {
      return of(profilingService.getProfilingDataProducer(eventType));
    }

    // In case the profiling feature is not enabled there is no data producer.
    return empty();
  }


  @Override
  public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
    ComponentLocation location = getLocation(processor);
    String artifactId = muleContext.getConfiguration().getId();
    String artifactType = muleContext.getArtifactType().getAsString();


    return pub -> subscriberContext()
        .flatMapMany(ctx -> {
          if (isTxActive(ctx)) {
            // The profiling events related to the processing strategy scheduling are triggered independently of this being
            // a blocking processing strategy that does not involve a thread switch.
            return buildFlux(pub)
                .profileEvent(location, getDataProducer(PS_SCHEDULING_OPERATION_EXECUTION), artifactId, artifactType)
                .profileEvent(location, getDataProducer(STARTING_OPERATION_EXECUTION), artifactId, artifactType)
                .transform(BLOCKING_PROCESSING_STRATEGY_INSTANCE.onProcessor(processor))
                .profileEvent(location, getDataProducer(OPERATION_EXECUTED), artifactId, artifactType)
                .profileEvent(location, getDataProducer(PS_FLOW_MESSAGE_PASSING), artifactId, artifactType)
                .build();
          } else {
            return from(pub).transform(delegate.onProcessor(processor));
          }
        });
  }

  private boolean isTxActive(Context ctx) {
    return ctx.<Deque<String>>getOrEmpty(TX_SCOPES_KEY).map(txScopes -> !txScopes.isEmpty()).orElse(false);
  }

  /**
   * Cleanup the state set by {@link #pushTxToSubscriberContext(String)}.
   *
   * @since 4.3
   */
  public static Function<Context, Context> popTxFromSubscriberContext() {
    return context -> {
      Deque<String> currentTxChains = new ArrayDeque<>(context.getOrDefault(TX_SCOPES_KEY, emptyList()));
      currentTxChains.pop();
      return context.put(TX_SCOPES_KEY, currentTxChains);
    };
  }

  /**
   * Force the upstream publisher to behave as if a transaction were active, effectively avoiding thread switches.
   *
   * @since 4.3
   */
  public static Function<Context, Context> pushTxToSubscriberContext(String location) {
    return context -> {
      Deque<String> currentTxChains = new ArrayDeque<>(context.getOrDefault(TX_SCOPES_KEY, emptyList()));
      currentTxChains.push(location);
      return context.put(TX_SCOPES_KEY, currentTxChains);
    };
  }

}
