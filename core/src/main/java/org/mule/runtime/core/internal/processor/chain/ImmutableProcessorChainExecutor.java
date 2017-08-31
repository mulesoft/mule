/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.chain;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.processor.MessageProcessors.processWithChildContext;
import static reactor.core.publisher.Mono.from;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.processor.EventBasedResult;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.Chain;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * An implementation of {@link Chain} that wraps a {@link Processor} and allows to execute it
 *
 * @since 4.0
 */
public class ImmutableProcessorChainExecutor implements Chain, Initialisable {

  /**
   * Processor that will be executed upon calling process
   */
  private MessageProcessorChain chain;

  /**
   * Event that will be cloned for dispatching
   */
  private final InternalEvent originalEvent;

  private InternalEvent currentEvent;

  private Consumer<Result> successHandler;
  private BiConsumer<Throwable, Result> errorHandler;

  /**
   * Creates a new immutable instance
   *
   * @param event the original {@link InternalEvent} for the execution of the given chain
   * @param chain a {@link Processor} chain to be executed
   */
  public ImmutableProcessorChainExecutor(InternalEvent event,
                                         MessageProcessorChain chain) {

    this.originalEvent = event;
    this.currentEvent = event;
    this.chain = chain;
  }


  @Override
  public void process() {
    doProcess(originalEvent);
  }

  @Override
  public void process(Object payload, Object attributes) {
    doProcess(InternalEvent.builder(originalEvent)
        .message(Message.builder()
            .payload(TypedValue.of(payload))
            .attributes(TypedValue.of(attributes))
            .build())
        .build());
  }

  @Override
  public void process(Result result) {
    if (result instanceof EventBasedResult) {
      this.currentEvent = ((EventBasedResult) result).getEvent();
      doProcess(currentEvent);
    } else {
      process(result.getOutput(), result.getAttributes());
    }
  }

  @Override
  public Chain onSuccess(Consumer<Result> onSuccess) {
    this.successHandler = onSuccess;
    return this;
  }

  @Override
  public Chain onError(BiConsumer<Throwable, Result> onError) {
    this.errorHandler = onError;
    return this;
  }

  private void doProcess(InternalEvent updatedEvent) {
    from(processWithChildContext(updatedEvent, chain, ofNullable(chain.getLocation())))
        .doOnSuccess(this::handleSuccess)
        .doOnError(MessagingException.class, error -> this.handleError(error, error.getEvent()))
        .doOnError(error -> this.handleError(error, currentEvent))
        .subscribe();
  }

  private void handleSuccess(InternalEvent childEvent) {
    if (successHandler != null && childEvent != null) {
      try {
        successHandler.accept(EventBasedResult.from(childEvent));
      } catch (Throwable e) {
        // The handler failed, we need to communicate the error to the parent stream
        originalEvent.getContext().error(e);
      }
    } else {
      // No handler, we need to signal completion in some way
      originalEvent.getContext().success(childEvent);
    }
  }

  private InternalEvent handleError(Throwable error, InternalEvent childEvent) {
    if (errorHandler == null) {
      // If no handler, then just propagate the error on for the parent event
      originalEvent.getContext().error(error);
    } else {
      try {
        errorHandler.accept(error, EventBasedResult.from(childEvent));
      } catch (Throwable e) {
        // The handler failed, we need to communicate the error to the parent stream
        originalEvent.getContext().error(e);
      }
    }

    return null;
  }

  @Override
  public void initialise() throws InitialisationException {
    LifecycleUtils.initialiseIfNeeded(chain);
  }

}
