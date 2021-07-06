/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_DIAGNOSTICS_SERVICE;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.OPERATION_EXECUTED;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_DISPATCH;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory.BLOCKING_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ReactorPublisherBuilder.buildFlux;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.subscriberContext;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.diagnostics.DiagnosticsService;
import org.mule.runtime.core.api.diagnostics.ProfilingDataProducer;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.processor.chain.InterceptedReactiveProcessor;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

import org.mule.runtime.core.privileged.processor.chain.HasLocation;
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
  public static final String UNKNOWN_FLOW = "unknown_flow";

  @Inject
  private DiagnosticsService diagnosticsService;

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
    if (featureFlags.isEnabled(ENABLE_DIAGNOSTICS_SERVICE)) {
      ComponentLocation location = getLocation(pipeline);
      ProfilingDataProducer hookFlowDispatch = diagnosticsService.getProfilingDataProducer(PS_FLOW_DISPATCH);
      ProfilingDataProducer hookFlowEnd = diagnosticsService.getProfilingDataProducer(PS_FLOW_DISPATCH);

      String artifactId = muleContext.getConfiguration().getId();
      String artifactType = muleContext.getArtifactType().getAsString();

      return pub -> subscriberContext()
          .flatMapMany(ctx -> {
            if (isTxActive(ctx)) {
              return buildFlux(pub)
                  .profileEvent(location, ofNullable(hookFlowDispatch), artifactId, artifactType)
                  .transform(BLOCKING_PROCESSING_STRATEGY_INSTANCE.onPipeline(pipeline))
                  .profileEvent(location, ofNullable(hookFlowEnd), artifactId, artifactType)
                  .build();
            } else {
              return from(pub).transform(delegate.onPipeline(pipeline));
            }
          });
    } else {
      return pub -> subscriberContext()
          .flatMapMany(ctx -> {
            if (isTxActive(ctx)) {
              return from(pub).transform(BLOCKING_PROCESSING_STRATEGY_INSTANCE.onPipeline(pipeline));
            } else {
              return from(pub).transform(delegate.onPipeline(pipeline));
            }
          });
    }
  }


  @Override
  public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
    if (featureFlags.isEnabled(ENABLE_DIAGNOSTICS_SERVICE)) {
      ProfilingDataProducer startingOperationExecutionHook =
          diagnosticsService.getProfilingDataProducer(STARTING_OPERATION_EXECUTION);
      ProfilingDataProducer operationExecutedHook = diagnosticsService.getProfilingDataProducer(OPERATION_EXECUTED);
      ProfilingDataProducer psSchedulingOperationExecution =
          diagnosticsService.getProfilingDataProducer(PS_SCHEDULING_OPERATION_EXECUTION);
      ProfilingDataProducer psFlowMessagePassing = diagnosticsService.getProfilingDataProducer(PS_FLOW_MESSAGE_PASSING);

      String artifactId = muleContext.getConfiguration().getId();
      String artifactType = muleContext.getArtifactType().getAsString();

      return pub -> subscriberContext()
          .flatMapMany(ctx -> {
            if (isTxActive(ctx)) {
              return buildFlux(pub)
                  .profileEvent(getLocation(processor), ofNullable(psSchedulingOperationExecution), artifactId, artifactType)
                  .profileEvent(getLocation(processor), ofNullable(startingOperationExecutionHook), artifactId, artifactType)
                  .transform(BLOCKING_PROCESSING_STRATEGY_INSTANCE.onProcessor(processor))
                  .profileEvent(getLocation(processor), ofNullable(operationExecutedHook), artifactId, artifactType)
                  .profileEvent(getLocation(processor), ofNullable(psFlowMessagePassing), artifactId, artifactType)
                  .build();
            } else {
              return from(pub).transform(delegate.onProcessor(processor));
            }
          });
    } else {
      return pub -> subscriberContext()
          .flatMapMany(ctx -> {
            if (isTxActive(ctx)) {
              return from(pub).transform(BLOCKING_PROCESSING_STRATEGY_INSTANCE.onProcessor(processor));
            } else {
              return from(pub).transform(delegate.onProcessor(processor));
            }
          });
    }
  }

  private ComponentLocation getLocation(ReactiveProcessor processor) {
    if (processor instanceof HasLocation) {
      return ((HasLocation) processor).resolveLocation();
    }
    if (processor instanceof InterceptedReactiveProcessor) {
      return getLocation(((InterceptedReactiveProcessor) processor).getProcessor());
    }

    if (processor instanceof Component) {
      return ((Component) processor).getLocation();
    }


    return null;
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
