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
import static org.mule.runtime.core.api.util.concurrent.FunctionalReadWriteLock.readWriteLock;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;

import org.mule.runtime.api.component.execution.CompletableCallback;
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

import reactor.core.publisher.FluxSink;

/**
 * Common behavior for flow dispatching, whether policies are applied or not.
 */
class CommonSourcePolicy {

  public static final String POLICY_SOURCE_PARAMETERS_PROCESSOR = "policy.source.parametersProcessor";
  public static final String POLICY_SOURCE_PROCESS_CALLBACK = "policy.source.processCallback";

  private final FluxSinkSupplier<CoreEvent> policySink;
  private final AtomicBoolean disposed;
  private final FunctionalReadWriteLock readWriteLock = readWriteLock();
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
    this.disposed = new AtomicBoolean(false);
    this.errorTypeRepository = new DefaultErrorTypeRepository();
  }

  public void process(CoreEvent sourceEvent,
                      MessageSourceResponseParametersProcessor respParamProcessor,
                      CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> callback) {

    readWriteLock.withReadLock(() -> {
      if (!disposed.get()) {
        policySink.get().next(quickCopy(sourceEvent, of(POLICY_SOURCE_PARAMETERS_PROCESSOR, respParamProcessor,
                                                        POLICY_SOURCE_PROCESS_CALLBACK, callback)));
      } else {
        MessagingException me = new MessagingException(createStaticMessage("Source policy already disposed"), sourceEvent);
        me.setProcessedEvent(CoreEvent.builder(sourceEvent)
            .error(ErrorBuilder.builder(me).errorType(errorTypeRepository.getAnyErrorType()).build()).build());

        Supplier<Map<String, Object>> errorParameters = sourcePolicyParametersTransformer.isPresent()
            ? (() -> sourcePolicyParametersTransformer.get().fromMessageToErrorResponseParameters(sourceEvent.getMessage()))
            : (() -> respParamProcessor.getFailedExecutionResponseParametersFunction().apply(sourceEvent));

        SourcePolicyFailureResult result = new SourcePolicyFailureResult(me, errorParameters);
        callback.complete(left(result));
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

    recoverCallback(event).complete(result);
  }

  public void finishFlowProcessing(CoreEvent event, Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result,
                                   Throwable error) {
    boolean isMessagingException = error instanceof MessagingException;
    if (!(isMessagingException && ((MessagingException) error).handled())) {

      if (!((BaseEventContext) event.getContext()).isComplete()) {
        ((BaseEventContext) event.getContext()).error(error);
      }

      recoverCallback(event).complete(result);
      if (isMessagingException) {
        ((MessagingException) error).setHandled(Boolean.TRUE);
      }
    }
  }

  private CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> recoverCallback(CoreEvent event) {
    return ((InternalEvent) event).getInternalParameter(POLICY_SOURCE_PROCESS_CALLBACK);
  }

  public void dispose() {
    readWriteLock.withWriteLock(() -> {
      policySink.dispose();
      disposed.set(true);
    });
  }
}
