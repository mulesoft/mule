/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.lang.Runtime.getRuntime;
import static org.mule.runtime.core.internal.policy.SourcePolicyContext.from;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.util.rx.FluxSinkSupplier;
import org.mule.runtime.core.internal.util.rx.RoundRobinFluxSinkSupplier;
import org.mule.runtime.core.internal.util.rx.TransactionAwareFluxSinkSupplier;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.function.Supplier;

import reactor.core.publisher.FluxSink;

/**
 * Common behavior for flow dispatching, whether policies are applied or not.
 */
class CommonSourcePolicy {

  private final FluxSinkSupplier<CoreEvent> policySink;

  CommonSourcePolicy(Supplier<FluxSink<CoreEvent>> sinkFactory) {
    this.policySink =
        new TransactionAwareFluxSinkSupplier<>(sinkFactory,
                                               new RoundRobinFluxSinkSupplier<>(getRuntime().availableProcessors(), sinkFactory));
  }

  public void process(CoreEvent sourceEvent,
                      MessageSourceResponseParametersProcessor respParamProcessor,
                      CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> callback) {

    SourcePolicyContext ctx = from(sourceEvent);
    if (ctx != null) {
      ctx.configure(respParamProcessor, callback);
    }

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

  public void dispose() {
    policySink.dispose();
  }

}
