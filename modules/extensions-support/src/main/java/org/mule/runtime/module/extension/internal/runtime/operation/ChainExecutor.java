/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedResult;
import org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContextDontComplete;
import static org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext.from;

class ChainExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChainExecutor.class);

  private final CoreEvent event;
  private final CoreEvent originalEvent;
  private final MessageProcessorChain chain;
  private final Consumer<Result> successHandler;
  private final BiConsumer<Throwable, Result> errorHandler;

  ChainExecutor(MessageProcessorChain chain,
                CoreEvent originalEvent, CoreEvent event,
                Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    this.chain = chain;
    this.event = event;
    this.originalEvent = originalEvent;
    this.successHandler = onSuccess;
    this.errorHandler = onError;
  }

  public void execute() {
    final SdkInternalContext sdkInternalCtx = from(event);
    Function<Context, Context> innerChainCtxMapping = identity();
    if (sdkInternalCtx != null) {
      innerChainCtxMapping = sdkInternalCtx.getInnerChainSubscriberContextMapping();
    }

    Mono.from(processWithChildContextDontComplete(event, chain, ofNullable(chain.getLocation())))
        .doOnSuccess(this::handleSuccess)
        .doOnError(error -> {
          if (error instanceof MessagingException) {
            this.handleError(error, ((MessagingException) error).getEvent());
          } else {
            LOGGER.error("Exception in nested chain", error);
            this.handleError(error, event);
          }
        })
        .subscriberContext(innerChainCtxMapping)
        .subscribe();
  }

  private void handleSuccess(CoreEvent childEvent) {
    Result result = childEvent != null ? EventedResult.from(childEvent) : Result.builder().build();
    try {
      successHandler.accept(result);
    } catch (Throwable error) {
      errorHandler.accept(error, result);
    }
  }

  private CoreEvent handleError(Throwable error, CoreEvent childEvent) {
    try {
      errorHandler.accept(error, EventedResult.from(childEvent));
    } catch (Throwable e) {
      ((BaseEventContext) originalEvent.getContext()).error(e);
    }
    return null;
  }


}
