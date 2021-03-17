/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContextDontComplete;
import static org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext.from;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedResult;
import org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

class ChainExecutor {

  private static final Logger LOGGER = getLogger(ChainExecutor.class);

  private final CoreEvent originalEvent;
  private final MessageProcessorChain chain;

  ChainExecutor(MessageProcessorChain chain, CoreEvent originalEvent) {
    this.chain = chain;
    this.originalEvent = originalEvent;
  }

  public void execute(CoreEvent event, Consumer<Result> successHandler, BiConsumer<Throwable, Result> errorHandler) {
    final SdkInternalContext sdkInternalCtx = from(event);
    Function<Context, Context> innerChainCtxMapping = identity();
    if (sdkInternalCtx != null) {
      innerChainCtxMapping = sdkInternalCtx.getInnerChainSubscriberContextMapping();
    }

    // TODO: MULE-19269 - Potential need to migrate this to a flux
    Mono.from(processWithChildContextDontComplete(event, chain, ofNullable(chain.getLocation())))
        .doOnSuccess(childEvent -> handleSuccess(childEvent, successHandler, errorHandler))
        .doOnError(error -> {
          if (error instanceof MessagingException) {
            this.handleError(error, ((MessagingException) error).getEvent(), errorHandler);
          } else {
            LOGGER.error("Exception in nested chain", error);
            this.handleError(error, event, errorHandler);
          }
        })
        .subscriberContext(innerChainCtxMapping)
        .subscribe();
  }

  private void handleSuccess(CoreEvent childEvent, Consumer<Result> successHandler, BiConsumer<Throwable, Result> errorHandler) {
    Result result = childEvent != null ? EventedResult.from(childEvent) : Result.builder().build();
    try {
      successHandler.accept(result);
    } catch (Throwable error) {
      errorHandler.accept(error, result);
    }
  }

  private CoreEvent handleError(Throwable error, CoreEvent childEvent, BiConsumer<Throwable, Result> errorHandler) {
    try {
      errorHandler.accept(error, EventedResult.from(childEvent));
    } catch (Throwable e) {
      ((BaseEventContext) originalEvent.getContext()).error(e);
    }
    return null;
  }


}
