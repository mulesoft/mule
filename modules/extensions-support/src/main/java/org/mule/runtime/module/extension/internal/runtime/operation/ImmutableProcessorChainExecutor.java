/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContextDontComplete;
import static org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext.from;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.HasMessageProcessors;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedResult;
import org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.util.context.Context;

/**
 * An implementation of {@link Chain} that wraps a {@link Processor} and allows to execute it
 *
 * @since 4.0
 */
public class ImmutableProcessorChainExecutor implements Chain, HasMessageProcessors {

  /**
   * Processor that will be executed upon calling process
   */
  private final MessageProcessorChain chain;

  /**
   * Event that will be cloned for dispatching
   */
  private final CoreEvent originalEvent;

  /**
   * Creates a new immutable instance
   *
   * @param event the original {@link CoreEvent} for the execution of the given chain
   * @param chain a {@link Processor} chain to be executed
   */
  public ImmutableProcessorChainExecutor(CoreEvent event, MessageProcessorChain chain) {
    this.originalEvent = event;
    this.chain = chain;
  }

  @Override
  public void process(Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    doProcess(originalEvent, onSuccess, onError);
  }

  @Override
  public void process(Object payload, Object attributes, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    CoreEvent customEvent = CoreEvent.builder(originalEvent)
        .message(Message.builder()
            .payload(TypedValue.of(payload))
            .attributes(TypedValue.of(attributes))
            .build())
        .build();

    doProcess(customEvent, onSuccess, onError);
  }

  @Override
  public void process(Result result, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    if (result instanceof EventedResult) {
      doProcess(((EventedResult) result).getEvent(), onSuccess, onError);
    } else {
      process(result.getOutput(), result.getAttributes(), onSuccess, onError);
    }
  }

  private void doProcess(CoreEvent event, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    checkArgument(onSuccess != null,
                  "A success completion handler is required in order to execute the components chain, but it was null");
    checkArgument(onError != null,
                  "An error completion handler is required in order to execute the components chain, but it was null");
    new Executor(chain, originalEvent, event, onSuccess, onError)
        .execute();
  }

  @Override
  public List<Processor> getMessageProcessors() {
    return chain.getMessageProcessors();
  }

  public CoreEvent getOriginalEvent() {
    return originalEvent;
  }

  private static final class Executor {

    private final CoreEvent event;
    private final CoreEvent originalEvent;
    private final MessageProcessorChain chain;
    private final Consumer<Result> successHandler;
    private final BiConsumer<Throwable, Result> errorHandler;

    Executor(MessageProcessorChain chain,
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

      from(processWithChildContextDontComplete(event, chain, ofNullable(chain.getLocation())))
          .doOnSuccess(this::handleSuccess)
          .doOnError(error -> {
            if (error instanceof MessagingException) {
              this.handleError(error, ((MessagingException) error).getEvent());
            } else {
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
}
