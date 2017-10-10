/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.HasMessageProcessors;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedResult;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * An implementation of {@link Chain} that wraps a {@link Processor} and allows to execute it
 *
 * @since 4.0
 */
public class ImmutableProcessorChainExecutor implements Chain, Initialisable, HasMessageProcessors {

  /**
   * Processor that will be executed upon calling process
   */
  private MessageProcessorChain chain;

  /**
   * Event that will be cloned for dispatching
   */
  private final CoreEvent originalEvent;

  private CoreEvent currentEvent;
  private Consumer<Result> successHandler;
  private BiConsumer<Throwable, Result> errorHandler;

  /**
   * Creates a new immutable instance
   *
   * @param event the original {@link CoreEvent} for the execution of the given chain
   * @param chain a {@link Processor} chain to be executed
   */
  public ImmutableProcessorChainExecutor(CoreEvent event, MessageProcessorChain chain) {
    this.originalEvent = event;
    this.currentEvent = event;
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
      this.currentEvent = ((EventedResult) result).getEvent();
      doProcess(currentEvent, onSuccess, onError);
    } else {
      process(result.getOutput(), result.getAttributes(), onSuccess, onError);
    }
  }

  private void setHandlers(Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    checkArgument(onSuccess != null,
                  "A success completion handler is required in order to execute the components chain, but it was null");
    checkArgument(onError != null,
                  "An error completion handler is required in order to execute the components chain, but it was null");

    this.successHandler = onSuccess;
    this.errorHandler = onError;
  }

  private void doProcess(CoreEvent updatedEvent, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    setHandlers(onSuccess, onError);
    from(processWithChildContext(updatedEvent, chain, ofNullable(chain.getLocation())))
        .doOnSuccess(this::handleSuccess)
        .doOnError(MessagingException.class, error -> this.handleError(error, error.getEvent()))
        .doOnError(error -> this.handleError(error, currentEvent))
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

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(chain);
  }

  @Override
  public List<Processor> getMessageProcessors() {
    return chain.getMessageProcessors();
  }
}
