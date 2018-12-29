/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static com.google.common.collect.ImmutableMap.of;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.currentThread;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static reactor.core.publisher.Mono.create;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

import cn.danielw.fop.ObjectFactory;
import cn.danielw.fop.ObjectPool;
import cn.danielw.fop.PoolConfig;
import cn.danielw.fop.Poolable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Schedulers;

/**
 * {@link SourcePolicy} created when no policies have to be applied.
 *
 * @since 4.0
 */
public class NoSourcePolicy implements SourcePolicy, Disposable {

  private static final Logger LOGGER = LoggerFactory.getLogger(NoSourcePolicy.class);

  public static final String POLICY_SOURCE_PARAMETERS_PROCESSOR = "policy.source.parametersProcessor";
  public static final String POLICY_SOURCE_CALLER_SINK = "policy.source.callerSink";

  private final ObjectPool<FluxSink<CoreEvent>> pool;

  public NoSourcePolicy(ReactiveProcessor flowExecutionProcessor) {
    PoolConfig config = new PoolConfig();
    config.setPartitionSize(getRuntime().availableProcessors());
    config.setMaxSize(1);
    config.setMinSize(1);
    config.setMaxIdleMilliseconds(MAX_VALUE);

    pool = new ObjectPool<>(config, new SourceFluxObjectFactory(flowExecutionProcessor));
  }

  private static final class SourceFluxObjectFactory implements ObjectFactory<FluxSink<CoreEvent>> {

    private final ReactiveProcessor flowExecutionProcessor;

    public SourceFluxObjectFactory(ReactiveProcessor flowExecutionProcessor) {
      this.flowExecutionProcessor = flowExecutionProcessor;
    }

    @Override
    public FluxSink<CoreEvent> create() {
      AtomicReference<FluxSink<CoreEvent>> sinkRef = new AtomicReference<>();

      Flux<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> policyFlux =
          Flux.<CoreEvent>create(sink -> sinkRef.set(sink))
              .parallel()
              .runOn(Schedulers.immediate())
              .composeGroup(flowExecutionProcessor)
              .map(flowExecutionResult -> {
                MessageSourceResponseParametersProcessor parametersProcessor =
                    ((InternalEvent) flowExecutionResult).getInternalParameter(POLICY_SOURCE_PARAMETERS_PROCESSOR);

                return right(SourcePolicyFailureResult.class,
                             new SourcePolicySuccessResult(flowExecutionResult,
                                                           () -> parametersProcessor
                                                               .getSuccessfulExecutionResponseParametersFunction()
                                                               .apply(flowExecutionResult),
                                                           parametersProcessor));
              })
              .doOnNext(result -> result.apply(spfr -> {
                final InternalEvent event = (InternalEvent) spfr.getMessagingException().getEvent();
                if (!event.getContext().isComplete()) {
                  event.getContext().error(spfr.getMessagingException());
                }
                ((MonoSink<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>) event
                    .getInternalParameter(POLICY_SOURCE_CALLER_SINK)).success(result);
              }, spsr -> {
                final InternalEvent event = (InternalEvent) spsr.getResult();
                if (!event.getContext().isComplete()) {
                  event.getContext().success(event);
                }
                ((MonoSink<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>) event
                    .getInternalParameter(POLICY_SOURCE_CALLER_SINK)).success(result);
              }))
              .sequential()
              .onErrorContinue((t, e) -> {
                final MessagingException me = (MessagingException) t;
                final InternalEvent event = (InternalEvent) me.getEvent();

                if (!event.getContext().isComplete()) {
                  event.getContext().error(t);
                }

                MessageSourceResponseParametersProcessor parametersProcessor =
                    event.getInternalParameter(POLICY_SOURCE_PARAMETERS_PROCESSOR);

                ((MonoSink<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>) event
                    .getInternalParameter(POLICY_SOURCE_CALLER_SINK))
                        .success(left(new SourcePolicyFailureResult(me, () -> parametersProcessor
                            .getFailedExecutionResponseParametersFunction()
                            .apply(me.getEvent()))));
              });

      policyFlux.subscribe();
      return sinkRef.get();
    }

    @Override
    public void destroy(FluxSink<CoreEvent> t) {
      t.complete();
    }

    @Override
    public boolean validate(FluxSink<CoreEvent> t) {
      return !t.isCancelled();
    }

  }


  @Override
  public Publisher<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> process(CoreEvent sourceEvent,
                                                                                         MessageSourceResponseParametersProcessor respParamProcessor) {
    return create(callerSink -> {
      try (Poolable<FluxSink<CoreEvent>> noPolicySink = pool.borrowObject()) {
        noPolicySink.getObject().next(quickCopy(sourceEvent, of(POLICY_SOURCE_PARAMETERS_PROCESSOR, respParamProcessor,
                                                                POLICY_SOURCE_CALLER_SINK, callerSink)));
      }
    });
  }

  @Override
  public void dispose() {
    try {
      pool.shutdown();
    } catch (InterruptedException e) {
      LOGGER.debug("Pool shutdown interrupted.");
      currentThread().interrupt();
    }
  }
}
