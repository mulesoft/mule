/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.lang.Runtime.getRuntime;
import static org.mule.runtime.core.api.util.func.Once.of;
import static org.mule.runtime.core.internal.policy.SourcePolicyContext.from;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.func.Once.ConsumeOnce;
import org.mule.runtime.core.internal.util.rx.FluxSinkSupplier;
import org.mule.runtime.core.internal.util.rx.RoundRobinFluxSinkSupplier;
import org.mule.runtime.core.internal.util.rx.TransactionAwareFluxSinkSupplier;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;

/**
 * Common behavior for flow dispatching, whether policies are applied or not.
 */
class CommonSourcePolicy {

  private static final Logger LOGGER = getLogger(CommonSourcePolicy.class);
  private final FluxSinkSupplier<CoreEvent> policySink;
  private final AtomicInteger inFlightEvents = new AtomicInteger(0);
  private ConsumeOnce<CommonSourcePolicy> onDrain = of(commonSourcePolicy -> {
  });
  private final BiConsumer<CoreEvent, Throwable> inflightDecrementCallback = (coreEvent, throwable) -> {
    int decremented = inFlightEvents.decrementAndGet();
    LOGGER.debug("Decremented inFlightEvents={}", decremented);
    if (decremented == 0) {
      onDrain.consumeOnce(this);
      LOGGER.debug("onDrain callback triggered");
    }
  };

  CommonSourcePolicy(FluxSinkSupplier<CoreEvent> sinkSupplier) {
    this.policySink =
        new TransactionAwareFluxSinkSupplier<>(sinkSupplier,
                                               new RoundRobinFluxSinkSupplier<>(getRuntime().availableProcessors(),
                                                                                sinkSupplier));
  }

  public void process(CoreEvent sourceEvent,
                      MessageSourceResponseParametersProcessor respParamProcessor,
                      CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> callback) {

    SourcePolicyContext ctx = from(sourceEvent);
    if (ctx != null) {
      ctx.configure(respParamProcessor, callback);
    }

    inFlightEvents.incrementAndGet();
    ((BaseEventContext) sourceEvent.getContext()).onTerminated(inflightDecrementCallback);
    policySink.get().next(sourceEvent);
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

  protected void drain(Consumer<CommonSourcePolicy> whenDrained) {
    onDrain = of(processingStrategy -> whenDrained.accept(this));
    inFlightEvents.getAndUpdate(operand -> {
      if (operand == 0) {
        onDrain.consumeOnce(this);
      }
      return operand;
    });
  }

  public void dispose() {
    policySink.dispose();
  }

  public Disposable deferredDispose() {
    return policySink;
  }

}
