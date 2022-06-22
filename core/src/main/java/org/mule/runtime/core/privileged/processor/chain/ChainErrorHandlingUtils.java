/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.chain;

import static java.util.function.Function.identity;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.ComposedErrorException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Provide standard methods to do error mapping/handling in a {@link MessageProcessorChain}.
 *
 * @since 4.3
 */
public final class ChainErrorHandlingUtils {

  private ChainErrorHandlingUtils() {
    // Nothing to do
  }

  /**
   * Used to catch exceptions emitted by reactor operators and wrap these in a MessagingException while conserving a reference to
   * the failed Event.
   */
  public static BiFunction<Throwable, Object, Throwable> getLocalOperatorErrorHook(Processor processor, ErrorTypeLocator locator,
                                                                                   Collection<ExceptionContextProvider> exceptionContextProviders) {
    final MessagingExceptionResolver exceptionResolver =
        (processor instanceof Component) ? new MessagingExceptionResolver((Component) processor) : null;
    final Function<MessagingException, MessagingException> messagingExceptionMapper =
        resolveMessagingException(processor, e -> exceptionResolver.resolve(e, locator, exceptionContextProviders));

    return (throwable, event) -> {
      throwable = unwrap(throwable);
      if (event instanceof CoreEvent) {
        if (throwable instanceof MessagingException) {
          return messagingExceptionMapper.apply((MessagingException) throwable);
        } else {
          return resolveException(processor, (CoreEvent) event, throwable, locator, exceptionContextProviders, exceptionResolver);
        }
      } else {
        return throwable;
      }
    };
  }

  static MessagingException resolveException(Processor processor, CoreEvent event, Throwable throwable,
                                             ErrorTypeLocator locator,
                                             Collection<ExceptionContextProvider> exceptionContextProviders,
                                             MessagingExceptionResolver exceptionResolver) {
    if (event.getError().isPresent()) {
      // Clear any current error from the event, so it doesn't contaminate the upcoming mapping
      event = CoreEvent.builder(event).error(null).build();
    }

    if (processor instanceof Component) {
      return exceptionResolver.resolve(new MessagingException(event, throwable, (Component) processor), locator,
                                       exceptionContextProviders);
    } else {
      return new MessagingException(event, throwable);
    }
  }

  static Function<MessagingException, MessagingException> resolveMessagingException(Processor processor,
                                                                                    Function<MessagingException, MessagingException> messagingExceptionMapper) {
    if (processor instanceof Component) {
      return exception -> {
        if (
        // Certain errors generated by routers have to be always resolved
        exception.getCause() instanceof RetryPolicyExhaustedException
            || exception.getCause() instanceof ComposedErrorException) {
          return messagingExceptionMapper.apply(exception);
        } else {
          return exception;
        }
      };
    } else {
      return identity();
    }
  }


}
