/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static com.google.common.collect.ImmutableMap.of;
import static java.lang.Runtime.getRuntime;
import static java.util.Optional.empty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static reactor.core.publisher.Mono.create;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.util.concurrent.FunctionalReadWriteLock;
import org.mule.runtime.core.internal.exception.DefaultErrorTypeRepository;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.ErrorBuilder;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.util.rx.FluxSinkSupplier;
import org.mule.runtime.core.internal.util.rx.RoundRobinFluxSinkSupplier;
import org.mule.runtime.core.internal.util.rx.TransactionAwareFluxSinkSupplier;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;

/**
 * Common behavior for flow dispatching, whether policies are applied or not.
 */
class CommonSourcePolicy {

  public static final String POLICY_SOURCE_PARAMETERS_PROCESSOR = "policy.source.parametersProcessor";
  public static final String POLICY_SOURCE_CALLER_SINK = "policy.source.callerSink";

  private final FluxSinkSupplier<CoreEvent> policySink;
  private final AtomicBoolean disposed;
  private final FunctionalReadWriteLock readWriteLock;
  private final Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer;
  private final DefaultErrorTypeRepository errorTypeRepository;

  CommonSourcePolicy(Supplier<FluxSink<CoreEvent>> sinkFactory) {
    this(sinkFactory, empty());
  }

  CommonSourcePolicy(Supplier<FluxSink<CoreEvent>> sinkFactory,
                     Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer) {
    this.policySink =
        new TransactionAwareFluxSinkSupplier<>(sinkFactory,
                                               new RoundRobinFluxSinkSupplier<>(getRuntime().availableProcessors(), sinkFactory));
    this.sourcePolicyParametersTransformer = sourcePolicyParametersTransformer;
    this.readWriteLock = FunctionalReadWriteLock.readWriteLock();
    this.disposed = new AtomicBoolean(false);
    this.errorTypeRepository = new DefaultErrorTypeRepository();
  }

  public Publisher<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> process(CoreEvent sourceEvent,
                                                                                         MessageSourceResponseParametersProcessor respParamProcessor) {
    return readWriteLock.withReadLock(lockReleaser -> {
      if (!disposed.get()) {
        return create(callerSink -> {
          policySink.get().next(quickCopy(sourceEvent, of(POLICY_SOURCE_PARAMETERS_PROCESSOR, respParamProcessor,
                                                          POLICY_SOURCE_CALLER_SINK, callerSink)));
        });
      } else {
        return just(sourceEvent)
            .map(event -> {
              MessagingException me = new MessagingException(createStaticMessage("Source policy already disposed"), sourceEvent);
              me.setProcessedEvent(CoreEvent.builder(sourceEvent)
                  .error(ErrorBuilder.builder(me).errorType(errorTypeRepository.getAnyErrorType()).build()).build());

              Supplier<Map<String, Object>> errorParameters = sourcePolicyParametersTransformer.isPresent()
                  ? (() -> sourcePolicyParametersTransformer.get().fromMessageToErrorResponseParameters(sourceEvent.getMessage()))
                  : (() -> respParamProcessor.getFailedExecutionResponseParametersFunction().apply(sourceEvent));

              ((BaseEventContext) event.getContext()).error(me);

              SourcePolicyFailureResult result = new SourcePolicyFailureResult(me, errorParameters);
              return left(result);
            });
      }
    });
  }

  public MessageSourceResponseParametersProcessor getResponseParamsProcessor(CoreEvent event) {
    return ((InternalEvent) event).getInternalParameter(POLICY_SOURCE_PARAMETERS_PROCESSOR);
  }

  public void finishFlowProcessing(CoreEvent event, Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result) {
    if (!((BaseEventContext) event.getContext()).isComplete()) {
      ((BaseEventContext) event.getContext()).success(event);
    }

    ((MonoSink<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>) ((InternalEvent) event)
        .getInternalParameter(POLICY_SOURCE_CALLER_SINK)).success(result);
  }

  public void finishFlowProcessing(CoreEvent event, Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result,
                                   Throwable error) {
    if (!((BaseEventContext) event.getContext()).isComplete()) {
      ((BaseEventContext) event.getContext()).error(error);
    }

    ((MonoSink<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>) ((InternalEvent) event)
        .getInternalParameter(POLICY_SOURCE_CALLER_SINK)).success(result);
  }

  public void dispose() {
    readWriteLock.withWriteLock(() -> {
      policySink.dispose();
      disposed.set(true);
    });
  }
}
