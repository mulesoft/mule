/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.api.message.error.matcher.ErrorTypeMatcherUtils.createErrorTypeMatcher;
import static org.mule.runtime.config.internal.error.MuleCoreErrorTypeRepository.CRITICAL_ERROR_TYPE;

import static reactor.core.publisher.Mono.error;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.error.matcher.ErrorTypeMatcher;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.exception.DefaultExceptionListener;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

/**
 * Handler that only accepts CRITICAL errors, logging them before propagating them. This handler is added before any others in all
 * error handlers.
 *
 * @since 4.0
 */
public class OnCriticalErrorHandler implements MessagingExceptionHandlerAcceptor {

  private static final Logger logger = LoggerFactory.getLogger(OnCriticalErrorHandler.class);

  private final ErrorTypeMatcher criticalMatcher = createErrorTypeMatcher(CRITICAL_ERROR_TYPE);
  private final ErrorTypeMatcher overloadMatcher;

  @Inject
  private NotificationDispatcher notificationFirer;

  private final DefaultExceptionListener exceptionListener;

  public OnCriticalErrorHandler(ErrorTypeMatcher overloadMatcher) {
    this.overloadMatcher = overloadMatcher;
    exceptionListener = new DefaultExceptionListener();
    exceptionListener.setNotificationFirer(notificationFirer);
    exceptionListener.setRepresentation(toString());
  }

  @Override
  public boolean accept(CoreEvent event) {
    Optional<Error> error = event.getError();
    return error.isPresent() && criticalMatcher.match(error.get().getErrorType());
  }

  @Override
  public boolean acceptsAll() {
    return false;
  }

  @Override
  public CoreEvent handleException(Exception exception, CoreEvent event) {
    logException(exception);
    return event;
  }

  @Override
  public Publisher<CoreEvent> apply(Exception exception) {
    logException(exception);
    return error(exception);
  }

  public void logException(Throwable exception) {
    if (exception instanceof MessagingException && ((MessagingException) exception).getEvent().getError().isPresent()) {
      ErrorType errorType = ((MessagingException) exception).getEvent().getError().get().getErrorType();
      if (overloadMatcher.match(errorType)) {
        logger.atInfo()
            .setMessage(() -> exceptionListener.resolveExceptionAndMessageToLog(exception).toString())
            .log();
        return;
      }
    }
    exceptionListener.resolveAndLogException(exception);
  }

  @Override
  public Consumer<Exception> router(Function<Publisher<CoreEvent>, Publisher<CoreEvent>> publisherPostProcessor,
                                    Consumer<CoreEvent> continueCallback,
                                    Consumer<Throwable> propagateCallback) {
    return error -> {
      logException(error);
      propagateCallback.accept(error);
    };
  }

  @Override
  public String toString() {
    return "OnCriticalErrorHandler";
  }

  public DefaultExceptionListener getExceptionListener() {
    return exceptionListener;
  }
}
