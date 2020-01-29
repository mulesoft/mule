/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * {@link SourcePolicy} created when no policies have to be applied.
 *
 * @since 4.0
 */
public class NoSourcePolicy implements SourcePolicy, Disposable, DeferredDisposable {

  private static final Logger LOGGER = getLogger(NoSourcePolicy.class);

  private final CommonSourcePolicy commonPolicy;

  public NoSourcePolicy(ReactiveProcessor flowExecutionProcessor) {
    commonPolicy = new CommonSourcePolicy(new SourceFluxObjectFactory(flowExecutionProcessor));
  }

  private final class SourceFluxObjectFactory implements Supplier<FluxSink<CoreEvent>> {

    private final ReactiveProcessor flowExecutionProcessor;

    public SourceFluxObjectFactory(ReactiveProcessor flowExecutionProcessor) {
      this.flowExecutionProcessor = flowExecutionProcessor;
    }

    @Override
    public FluxSink<CoreEvent> get() {
      final FluxSinkRecorder<CoreEvent> sinkRef = new FluxSinkRecorder<>();

      Flux<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> policyFlux =
          Flux.create(sinkRef)
              .transform(flowExecutionProcessor)
              .map(flowExecutionResult -> {
                MessageSourceResponseParametersProcessor parametersProcessor =
                    commonPolicy.getResponseParamsProcessor(flowExecutionResult);

                return right(SourcePolicyFailureResult.class,
                             new SourcePolicySuccessResult(flowExecutionResult,
                                                           () -> parametersProcessor
                                                               .getSuccessfulExecutionResponseParametersFunction()
                                                               .apply(flowExecutionResult),
                                                           parametersProcessor));
              })
              .doOnNext(result -> result.apply(spfr -> commonPolicy.finishFlowProcessing(spfr.getMessagingException().getEvent(),
                                                                                         result, spfr.getMessagingException()),
                                               spsr -> commonPolicy.finishFlowProcessing(spsr.getResult(), result)))
              .onErrorContinue(MessagingException.class, (t, e) -> {
                final MessagingException me = (MessagingException) t;
                final InternalEvent event = (InternalEvent) me.getEvent();

                commonPolicy.finishFlowProcessing(event,
                                                  left(new SourcePolicyFailureResult(me, () -> commonPolicy
                                                      .getResponseParamsProcessor(event)
                                                      .getFailedExecutionResponseParametersFunction().apply(me.getEvent()))),
                                                  me);
              });

      policyFlux.subscribe(null, e -> LOGGER.error("Exception reached subscriber for " + toString(), e));
      return sinkRef.getFluxSink();
    }

  }

  @Override
  public Publisher<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> process(CoreEvent sourceEvent,
                                                                                         MessageSourceResponseParametersProcessor respParamProcessor) {
    return commonPolicy.process(sourceEvent, respParamProcessor);
  }

  @Override
  public void dispose() {
    commonPolicy.dispose();
  }

  @Override
  public Disposable deferredDispose() {
    return () -> commonPolicy.dispose();
  }
}
