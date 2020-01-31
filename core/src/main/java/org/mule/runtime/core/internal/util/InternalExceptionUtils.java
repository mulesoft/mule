/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static java.util.Collections.emptyList;
import static org.mule.runtime.core.api.util.ExceptionUtils.getComponentIdentifier;
import static org.mule.runtime.core.api.util.ExceptionUtils.isUnknownMuleError;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.ErrorMapping;
import org.mule.runtime.core.internal.exception.ErrorMappingsAware;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.ErrorBuilder;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;

import java.util.List;

/**
 * Provides utilities to handle {@link MessagingException}s.
 *
 * @since 4.0
 */
public final class InternalExceptionUtils {

  private InternalExceptionUtils() {
    // Nothing to do
  }

  /**
   * Create new {@link CoreEvent} with {@link org.mule.runtime.api.message.Error} instance set.
   *
   * @param currentEvent event when error occurred.
   * @param obj message processor/source.
   * @param me messaging exception.
   * @param locator the mule context.
   * @return new {@link CoreEvent} with relevant {@link org.mule.runtime.api.message.Error} set.
   */
  public static CoreEvent createErrorEvent(CoreEvent currentEvent, Component obj,
                                           MessagingException me, ErrorTypeLocator locator) {
    Throwable cause = me.getCause() != null ? me.getCause() : me;

    List<ErrorMapping> errorMappings = emptyList();
    if (obj instanceof ErrorMappingsAware) {
      errorMappings = ((ErrorMappingsAware) obj).getErrorMappings();
    }

    if (!errorMappings.isEmpty() || isMessagingExceptionCause(me, cause)) {
      Error newError = getErrorFromFailingProcessor(me.getExceptionInfo().getErrorType(), obj, cause, locator);
      CoreEvent newEvent = quickCopy(newError, me.getEvent());
      me.setProcessedEvent(newEvent);
      return newEvent;
    } else {
      return currentEvent;
    }
  }

  private static boolean isMessagingExceptionCause(MessagingException me, Throwable cause) {
    return !me.getEvent().getError()
        .filter(error -> cause.equals(error.getCause()))
        .filter(error -> me.causedExactlyBy(error.getCause().getClass()))
        .isPresent();
  }

  /**
   * Determine the {@link ErrorType} of a given exception thrown by a given message processor.
   *
   * @param currentError the currently resolved error type.
   * @param cause the exception thrown.
   * @param locator the {@link ErrorTypeLocator}.
   * @return the resolved {@link ErrorType}
   */
  public static Error getErrorFromFailingProcessor(ErrorType currentError,
                                                   Component processor,
                                                   Throwable cause,
                                                   ErrorTypeLocator locator) {
    ErrorType foundErrorType = locator.lookupErrorType(cause);
    ErrorType resultError = isUnknownMuleError(foundErrorType)
        ? currentError
        : foundErrorType;

    ErrorType errorTypeLookedUp = getComponentIdentifier(processor)
        .map(ci -> locator.lookupComponentErrorType(ci, cause))
        .orElse(foundErrorType);

    ErrorType errorType = isUnknownMuleError(errorTypeLookedUp) && resultError != null
        ? resultError
        : errorTypeLookedUp;

    if (processor instanceof ErrorMappingsAware) {
      final List<ErrorMapping> errorMappings = ((ErrorMappingsAware) processor).getErrorMappings();

      if (errorMappings.isEmpty()) {
        return ErrorBuilder.builder(cause)
            .errorType(errorType)
            .build();
      } else {
        return ErrorBuilder.builder(cause)
            .errorType(errorMappings
                .stream()
                .filter(m -> m.match(resultError == null || isUnknownMuleError(resultError) ? errorType : currentError))
                .findFirst()
                .map(ErrorMapping::getTarget)
                .orElse(errorType))
            .build();
      }
    } else {
      return ErrorBuilder.builder(cause)
          .errorType(errorType)
          .build();
    }
  }
}
