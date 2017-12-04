/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.MessageProcessors;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedResult;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import reactor.core.publisher.Mono;

/**
 * Abstract base class for executors of {@link Processor} components
 * 
 * @since 4.1
 */
public abstract class AbstractProcessorExecutor {

  /**
   * Processor that will be executed upon calling process
   */
  protected final Processor processor;

  /**
   * Event that will be cloned for dispatching
   */
  protected final CoreEvent originalEvent;


  protected final ComponentLocation location;

  /**
   * Creates a new immutable instance
   *
   * @param event the original {@link CoreEvent} for the execution of the given chain
   * @param processor a {@link Processor} chain to be executed
   */
  public AbstractProcessorExecutor(CoreEvent event, Processor processor, ComponentLocation location) {
    this.originalEvent = event;
    this.processor = processor;
    this.location = location;
  }

  protected void doProcess(Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    doProcess(originalEvent, onSuccess, onError);
  }

  protected void doProcess(CoreEvent event, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    checkArgument(onSuccess != null,
                  "A success completion handler is required in order to execute the components chain, but it was null");
    checkArgument(onError != null,
                  "An error completion handler is required in order to execute the components chain, but it was null");
    new ProcessorExecutor(processor, location, originalEvent, event, onSuccess, onError)
        .execute();
  }

  private static class ProcessorExecutor {

    private final CoreEvent event;
    private final CoreEvent originalEvent;
    private final Processor processor;
    private final ComponentLocation location;
    private final Consumer<Result> successHandler;
    private final BiConsumer<Throwable, Result> errorHandler;

    ProcessorExecutor(Processor processor,
                      ComponentLocation location,
                      CoreEvent originalEvent, CoreEvent event,
                      Consumer<Result> onSuccess,
                      BiConsumer<Throwable, Result> onError) {
      this.processor = processor;
      this.event = event;
      this.location = location;
      this.originalEvent = originalEvent;
      this.successHandler = onSuccess;
      this.errorHandler = onError;
    }

    public void execute() {
      Mono.from(MessageProcessors.processWithChildContext(event, processor, Optional.ofNullable(location)))
          .doOnSuccess(this::handleSuccess)
          .doOnError(MessagingException.class, error -> this.handleError(error, error.getEvent()))
          .doOnError(error -> this.handleError(error, event))
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
