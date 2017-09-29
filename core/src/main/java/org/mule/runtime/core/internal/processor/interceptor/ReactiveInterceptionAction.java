/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.interceptor;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mule.runtime.core.internal.util.InternalExceptionUtils.getErrorFromFailingProcessor;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.interception.DefaultInterceptionEvent;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;

import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link InterceptionAction} that does the needed hooks with {@code Reactor} into the pipeline.
 *
 * @since 4.0
 */
class ReactiveInterceptionAction implements InterceptionAction {

  private static final Logger LOGGER = getLogger(ReactiveInterceptionAction.class);

  private ErrorTypeLocator errorTypeLocator;

  private Processor processor;
  private ReactiveProcessor next;
  private DefaultInterceptionEvent interceptionEvent;

  public ReactiveInterceptionAction(DefaultInterceptionEvent interceptionEvent,
                                    ReactiveProcessor next, Processor processor, ErrorTypeLocator errorTypeLocator) {
    this.interceptionEvent = interceptionEvent;
    this.next = next;
    this.processor = processor;
    this.errorTypeLocator = errorTypeLocator;
  }

  @Override
  public CompletableFuture<InterceptionEvent> proceed() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Called proceed() for processor {}", ((Component) processor).getLocation().getLocation());
    }

    return just(interceptionEvent.resolve())
        .cast(CoreEvent.class)
        .transform(next)
        .cast(InternalEvent.class)
        .map(event -> {
          interceptionEvent.reset(event);
          return interceptionEvent;
        })
        .cast(InterceptionEvent.class)
        .toFuture();
  }

  @Override
  public CompletableFuture<InterceptionEvent> skip() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Called skip() for processor {}", ((Component) processor).getLocation().getLocation());
    }

    interceptionEvent.resolve();
    return completedFuture(interceptionEvent);
  }

  @Override
  public CompletableFuture<InterceptionEvent> fail(Throwable cause) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Called fail() for processor {} with cause {} ({})", ((Component) processor).getLocation().getLocation(),
                   cause.getClass(), cause.getMessage());
    }

    Error newError = getErrorFromFailingProcessor(null, (Component) processor, cause, errorTypeLocator);

    interceptionEvent.setError(newError.getErrorType(), cause);
    CompletableFuture<InterceptionEvent> completableFuture = new CompletableFuture<>();
    completableFuture
        .completeExceptionally(new MessagingException(interceptionEvent.resolve(), cause, (Component) processor));
    return completableFuture;
  }

  @Override
  public CompletableFuture<InterceptionEvent> fail(ErrorType errorType) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Called fail() for processor {} with errorType {}", ((Component) processor).getLocation().getLocation(),
                   errorType.getIdentifier());
    }

    Throwable cause = new InterceptionException("");
    interceptionEvent.setError(errorType, cause);
    CompletableFuture<InterceptionEvent> completableFuture = new CompletableFuture<>();
    completableFuture
        .completeExceptionally(new MessagingException(interceptionEvent.resolve(), cause, (Component) processor));
    return completableFuture;
  }
}
