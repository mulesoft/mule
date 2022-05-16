/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.core.internal.policy.SourcePolicyContext.from;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.function.Supplier;

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
    commonPolicy = new CommonSourcePolicy(new SourceFluxObjectFactory(this, flowExecutionProcessor));
  }

  private static final class SourceFluxObjectFactory implements Supplier<FluxSink<CoreEvent>> {

    // private final Reference<NoSourcePolicy> noSourcePolicy;
    private final NoSourcePolicy noSourcePolicy;
    private final ReactiveProcessor flowExecutionProcessor;

    public SourceFluxObjectFactory(NoSourcePolicy noSourcePolicy, ReactiveProcessor flowExecutionProcessor) {
      // Avoid instances of this class from preventing the policy from being gc'd
      // Break the circular reference between policy-sinkFactory-flux that may cause memory leaks in the policies caches
      this.noSourcePolicy = noSourcePolicy;
      this.flowExecutionProcessor = flowExecutionProcessor;
    }

    @Override
    public FluxSink<CoreEvent> get() {
      final FluxSinkRecorder<CoreEvent> sinkRef = new FluxSinkRecorder<>();

      Flux<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> policyFlux =
          sinkRef.flux()
              .transform(flowExecutionProcessor)
              .map(flowExecutionResult -> {
                SourcePolicyContext ctx = from(flowExecutionResult);
                MessageSourceResponseParametersProcessor parametersProcessor = ctx.getResponseParametersProcessor();

                return right(SourcePolicyFailureResult.class,
                             new SourcePolicySuccessResult(flowExecutionResult,
                                                           () -> parametersProcessor
                                                               .getSuccessfulExecutionResponseParametersFunction()
                                                               .apply(flowExecutionResult),
                                                           parametersProcessor));
              })
              .doOnNext(result -> result.apply(spfr -> {
                CoreEvent event = spfr.getMessagingException().getEvent();
                SourcePolicyContext ctx = from(event);
                // NoSourcePolicy strongReference = noSourcePolicy.get();
                // if (strongReference != null) {
                // strongReference.commonPolicy.finishFlowProcessing(event, result, spfr.getMessagingException(), ctx);
                // }
                noSourcePolicy.commonPolicy.finishFlowProcessing(event, result, spfr.getMessagingException(), ctx);
              }, spsr -> {
                // NoSourcePolicy strongReference = noSourcePolicy.get();
                // if (strongReference != null) {
                // strongReference.commonPolicy.finishFlowProcessing(spsr.getResult(), result);
                // }
                noSourcePolicy.commonPolicy.finishFlowProcessing(spsr.getResult(), result);
              }))
              .onErrorContinue(MessagingException.class, (t, e) -> {
                final MessagingException me = (MessagingException) t;
                final InternalEvent event = (InternalEvent) me.getEvent();

                // NoSourcePolicy strongReference = noSourcePolicy.get();
                // if (strongReference != null) {
                // strongReference.commonPolicy.finishFlowProcessing(event,
                // left(new SourcePolicyFailureResult(me, () -> from(event)
                // .getResponseParametersProcessor()
                // .getFailedExecutionResponseParametersFunction()
                // .apply(me.getEvent()))),
                // me,
                // from(event));
                // }
                noSourcePolicy.commonPolicy.finishFlowProcessing(event,
                                                                 left(new SourcePolicyFailureResult(me, () -> from(event)
                                                                     .getResponseParametersProcessor()
                                                                     .getFailedExecutionResponseParametersFunction()
                                                                     .apply(me.getEvent()))),
                                                                 me,
                                                                 from(event));
              });

      policyFlux.subscribe(null, e -> LOGGER.error("Exception reached subscriber for {}", this, e));

      return sinkRef.getFluxSink();
    }

  }

  @Override
  public void process(CoreEvent sourceEvent,
                      MessageSourceResponseParametersProcessor respParamProcessor,
                      CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> callback) {
    commonPolicy.process(sourceEvent, respParamProcessor, callback);
  }

  @Override
  public void dispose() {
    commonPolicy.dispose();
  }

  @Override
  public Disposable deferredDispose() {
    return commonPolicy::dispose;
  }
}
