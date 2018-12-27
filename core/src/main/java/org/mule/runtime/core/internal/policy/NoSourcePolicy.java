/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static com.google.common.collect.ImmutableMap.of;
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

import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;

/**
 * {@link SourcePolicy} created when no policies have to be applied.
 *
 * @since 4.0
 */
public class NoSourcePolicy implements SourcePolicy, Disposable {

  public static final String POLICY_SOURCE_PARAMETERS_PROCESSOR = "policy.source.parametersProcessor";
  public static final String POLICY_SOURCE_CALLER_SINK = "policy.source.callerSink";

  private final reactor.core.Disposable fluxSubscription;
  private final FluxSink<CoreEvent> policySink;

  public NoSourcePolicy(ReactiveProcessor flowExecutionProcessor) {
    AtomicReference<FluxSink<CoreEvent>> sinkRef = new AtomicReference<>();

    Flux<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> policyFlux =
        Flux.<CoreEvent>create(sink -> sinkRef.set(sink))
            .transform(flowExecutionProcessor)
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
            }));

    fluxSubscription = policyFlux.subscribe();
    policySink = sinkRef.get();
  }

  @Override
  public Publisher<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> process(CoreEvent sourceEvent,
                                                                                         MessageSourceResponseParametersProcessor respParamProcessor) {
    return create(callerSink -> {
      policySink.next(quickCopy(sourceEvent, of(POLICY_SOURCE_PARAMETERS_PROCESSOR, respParamProcessor,
                                                POLICY_SOURCE_CALLER_SINK, callerSink)));
    });
  }

  @Override
  public void dispose() {
    policySink.complete();
    fluxSubscription.dispose();
  }
}
