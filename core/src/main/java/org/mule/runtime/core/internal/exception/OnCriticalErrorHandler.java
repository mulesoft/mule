/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.core.internal.exception.DefaultErrorTypeRepository.CRITICAL_ERROR_TYPE;
import static reactor.core.publisher.Mono.error;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.privileged.exception.AbstractExceptionListener;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;

import java.util.Optional;

import org.reactivestreams.Publisher;

/**
 * Handler that only accepts CRITICAL errors, logging them before propagating them. This handler is added before any others in all
 * error handlers.
 *
 * @since 4.0
 */
public class OnCriticalErrorHandler extends AbstractExceptionListener implements MessagingExceptionHandlerAcceptor {

  private final ErrorTypeMatcher criticalMatcher = new SingleErrorTypeMatcher(CRITICAL_ERROR_TYPE);
  private final ErrorTypeMatcher overloadMatcher;

  public OnCriticalErrorHandler(ErrorTypeMatcher overloadMatcher) {
    this.overloadMatcher = overloadMatcher;
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
        if (logger.isInfoEnabled()) {
          logger.info(resolveExceptionAndMessageToLog(exception).toString());
        }
        return;
      }
    }
    resolveAndLogException(exception);
  }

}
