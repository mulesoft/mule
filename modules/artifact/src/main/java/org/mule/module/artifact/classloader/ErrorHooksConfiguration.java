/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.exception.MessagingException;

import org.slf4j.Logger;
import reactor.core.publisher.Hooks;

/**
 * Registers reactor-core error handling hooks when dynamically loaded into each plugin class loader.
 *
 * IMPORTANT: this class is on a different package than the rest of the classes in this module. The reason of that is that this
 * class must be loaded by each artifact class loader that is being disposed. So, it cannot contain any of the prefixes that force
 * a class to be loaded from the container.
 *
 * @since 4.0
 */
public class ErrorHooksConfiguration {

  private static Logger logger;

  static {
    // Ensure reactor operatorError hook is always registered.
    Hooks.onOperatorError((throwable, signal) -> {
      // Unwrap all throwables to be consistent with reactor default hook.
      throwable = unwrap(throwable);
      // Only apply hook for Event signals.
      if (signal instanceof BaseEvent) {
        return throwable instanceof MessagingException ? throwable
            : new MessagingException((BaseEvent) signal, throwable);
      } else {
        return throwable;
      }
    });

    // Log dropped events/errors rather than blow up which causes cryptic timeouts and stack traces.
    Hooks.onErrorDropped(error -> logError("ERROR DROPPED UNEXPECTEDLY " + error));
    Hooks.onNextDropped(event -> logError("EVENT DROPPED UNEXPECTEDLY " + event));
  }

  private static void logError(String message) {
    if (logger == null) {
      synchronized (ErrorHooksConfiguration.class) {
        if (logger == null) {
          logger = getLogger(ErrorHooksConfiguration.class);
        }
      }
    }

    logger.error(message);
  }
}
