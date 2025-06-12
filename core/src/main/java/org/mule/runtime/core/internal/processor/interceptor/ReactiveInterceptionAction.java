/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import static org.mule.runtime.core.internal.exception.InternalExceptionUtils.getErrorFromFailingProcessor;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.WITHIN_PROCESS_TO_APPLY;

import static java.util.concurrent.CompletableFuture.completedFuture;

import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.event.InternalEvent;
import org.mule.runtime.core.internal.interception.DefaultInterceptionEvent;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.core.privileged.exception.MessagingException;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import reactor.util.context.ContextView;

/**
 * Implementation of {@link InterceptionAction} that does the needed hooks with {@code Reactor} into the pipeline.
 *
 * @since 4.0
 */
class ReactiveInterceptionAction implements InterceptionAction {

  private static final Logger LOGGER = getLogger(ReactiveInterceptionAction.class);

  private final ErrorTypeLocator errorTypeLocator;

  private final ReactiveProcessor processor;
  private final ReactiveProcessor next;
  private final ContextView ctx;
  private final DefaultInterceptionEvent interceptionEvent;

  public ReactiveInterceptionAction(DefaultInterceptionEvent interceptionEvent,
                                    ReactiveProcessor next, ContextView ctx, ReactiveProcessor processor,
                                    ErrorTypeLocator errorTypeLocator) {
    this.interceptionEvent = interceptionEvent;
    this.next = next;
    this.ctx = ctx;
    this.processor = processor;
    this.errorTypeLocator = errorTypeLocator;
  }

  @Override
  public CompletableFuture<InterceptionEvent> proceed() {
    LOGGER.atDebug()
        .setMessage("Called proceed() for processor {}")
        .addArgument(() -> ((Component) processor).getLocation().getLocation())
        .log();

    return just(interceptionEvent.resolve())
        .cast(CoreEvent.class)
        .transform(next)
        .cast(InternalEvent.class)
        .map(interceptionEvent::reset)
        .cast(InterceptionEvent.class)
        // This is needed for all cases because the invoked component may use fluxes
        .contextWrite(innerCtx -> innerCtx.put(WITHIN_PROCESS_TO_APPLY, true))
        .contextWrite(ctx)
        .toFuture();
  }

  @Override
  public CompletableFuture<InterceptionEvent> skip() {
    LOGGER.atDebug()
        .setMessage("Called skip() for processor {}")
        .addArgument(() -> ((Component) processor).getLocation().getLocation())
        .log();

    interceptionEvent.resolve();
    return completedFuture(interceptionEvent);
  }

  @Override
  public CompletableFuture<InterceptionEvent> fail(Throwable cause) {
    LOGGER.atDebug()
        .setMessage("Called fail() for processor {} with cause {} ({})")
        .addArgument(() -> ((Component) processor).getLocation().getLocation())
        .addArgument(cause.getClass())
        .addArgument(cause.getMessage())
        .log();

    Error newError = getErrorFromFailingProcessor(null, (Component) processor, cause, errorTypeLocator);

    interceptionEvent.setError(newError.getErrorType(), cause);
    CompletableFuture<InterceptionEvent> completableFuture = new CompletableFuture<>();
    completableFuture
        .completeExceptionally(new MessagingException(interceptionEvent.resolve(), cause, (Component) processor));
    return completableFuture;
  }

  private CompletableFuture<InterceptionEvent> failWithMessage(ErrorType errorType, String msg) {
    Throwable cause = new InterceptionException(msg);
    interceptionEvent.setError(errorType, cause);
    CompletableFuture<InterceptionEvent> completableFuture = new CompletableFuture<>();
    completableFuture.completeExceptionally(new MessagingException(interceptionEvent.resolve(), cause, (Component) processor));
    return completableFuture;
  }

  @Override
  public CompletableFuture<InterceptionEvent> fail(ErrorType errorType) {
    LOGGER.atDebug()
        .setMessage("Called fail() for processor {} with errorType {}")
        .addArgument(() -> ((Component) processor).getLocation().getLocation())
        .addArgument(errorType.getIdentifier())
        .log();
    return failWithMessage(errorType, "");
  }

  @Override
  public CompletableFuture<InterceptionEvent> fail(ErrorType errorType, String msg) {
    LOGGER.atDebug()
        .setMessage("Called fail() for processor {} with errorType {} and message {}")
        .addArgument(() -> ((Component) processor).getLocation().getLocation())
        .addArgument(errorType.getIdentifier())
        .addArgument(msg)
        .log();
    return failWithMessage(errorType, msg);
  }
}
