/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.core.api.functional.Either.left;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;

import org.reactivestreams.Publisher;

/**
 * {@link SourcePolicy} created when no policies have to be applied.
 *
 * @since 4.0
 */
public class NoSourcePolicy implements SourcePolicy {

  @Override
  public Publisher<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> process(CoreEvent sourceEvent,
                                                                                         ReactiveProcessor flowExecutionProcessor,
                                                                                         MessageSourceResponseParametersProcessor respParamProcessor) {
    return just(sourceEvent)
        .transform(flowExecutionProcessor)
        .map(flowExecutionResult -> Either.right(SourcePolicyFailureResult.class,
                                                 new SourcePolicySuccessResult(flowExecutionResult,
                                                                               () -> respParamProcessor
                                                                                   .getSuccessfulExecutionResponseParametersFunction()
                                                                                   .apply(flowExecutionResult),
                                                                               respParamProcessor)))
        .onErrorResume(MessagingException.class, messagingException -> {
          return just(left(new SourcePolicyFailureResult(messagingException, () -> respParamProcessor
              .getFailedExecutionResponseParametersFunction()
              .apply(messagingException.getEvent()))));
        })
        .doOnNext(result -> result.apply(spfr -> {
          final InternalEvent ev = (InternalEvent) spfr.getMessagingException().getEvent();
          if (!ev.getContext().isComplete()) {
            ev.getContext().error(spfr.getMessagingException());
          }
        }, spsr -> {
          final InternalEvent ev = (InternalEvent) spsr.getResult();
          if (!ev.getContext().isComplete()) {
            ev.getContext().success(ev);
          }
        }));
  }

}
