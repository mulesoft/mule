/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.processor.interceptor;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mule.runtime.core.api.util.ExceptionUtils.getErrorFromFailingProcessor;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.interception.DefaultInterceptionEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.exception.MessagingException;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link InterceptionAction} that does the needed hooks with {@code Reactor} into the pipeline.
 *
 * @since 4.0
 */
class ReactiveInterceptionAction implements InterceptionAction {

  private MuleContext muleContext;

  private Processor processor;
  private ReactiveProcessor next;
  private DefaultInterceptionEvent interceptionEvent;

  public ReactiveInterceptionAction(DefaultInterceptionEvent interceptionEvent,
                                    ReactiveProcessor next, Processor processor, MuleContext muleContext) {
    this.interceptionEvent = interceptionEvent;
    this.next = next;
    this.processor = processor;
    this.muleContext = muleContext;
  }

  @Override
  public CompletableFuture<InterceptionEvent> proceed() {
    return just(interceptionEvent.resolve())
        .transform(next)
        .map(event -> new DefaultInterceptionEvent(event))
        .cast(InterceptionEvent.class)
        .toFuture();
  }

  @Override
  public CompletableFuture<InterceptionEvent> skip() {
    interceptionEvent.resolve();
    return completedFuture(interceptionEvent);
  }

  @Override
  public CompletableFuture<InterceptionEvent> fail(Throwable cause) {
    Error newError = getErrorFromFailingProcessor(processor, cause, muleContext.getErrorTypeLocator());

    interceptionEvent.setError(newError.getErrorType(), cause);
    CompletableFuture<InterceptionEvent> completableFuture = new CompletableFuture<>();
    completableFuture.completeExceptionally(new MessagingException(interceptionEvent.resolve(), cause, processor));
    return completableFuture;
  }

  @Override
  public CompletableFuture<InterceptionEvent> fail(ErrorType errorType) {
    Throwable cause = new InterceptionException("");
    interceptionEvent.setError(errorType, cause);
    CompletableFuture<InterceptionEvent> completableFuture = new CompletableFuture<>();
    completableFuture.completeExceptionally(new MessagingException(interceptionEvent.resolve(), cause, processor));
    return completableFuture;
  }
}
