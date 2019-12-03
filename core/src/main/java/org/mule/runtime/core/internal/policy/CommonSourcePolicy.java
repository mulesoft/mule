/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.lang.Runtime.getRuntime;
import static java.util.Optional.empty;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.concurrent.FunctionalReadWriteLock.readWriteLock;
import static org.mule.runtime.core.internal.policy.SourcePolicyContext.from;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.util.concurrent.FunctionalReadWriteLock;
import org.mule.runtime.core.internal.exception.DefaultErrorTypeRepository;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.ErrorBuilder;
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
        SourcePolicyContext ctx = from(sourceEvent);
        if (ctx != null) {
          ctx.configure(respParamProcessor, callback);
        }

        policySink.get().next(sourceEvent);
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

  public void finishFlowProcessing(CoreEvent event, Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result) {
    if (!((BaseEventContext) event.getContext()).isComplete()) {
      ((BaseEventContext) event.getContext()).success(event);
    }

    from(event).getProcessCallback().complete(result);
  }

  public void finishFlowProcessing(CoreEvent event,
                                   Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result,
                                   Throwable error,
                                   SourcePolicyContext ctx) {
    if (!((BaseEventContext) event.getContext()).isComplete()) {
      ((BaseEventContext) event.getContext()).error(error);
    }

    ctx.getProcessCallback().complete(result);
  }

  public void dispose() {
    readWriteLock.withWriteLock(() -> {
      policySink.dispose();
      disposed.set(true);
    });
  }
}
