/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.ExceptionRouter;

import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import reactor.core.publisher.Mono;

/**
 * Base implementation for exception handlers.
 *
 * @since 4.2
 */
public abstract class BaseExceptionHandler implements FlowExceptionHandler {

  private static final Logger LOGGER = getLogger(BaseExceptionHandler.class);

  @Override
  public CoreEvent handleException(Exception exception, CoreEvent event) {
    onError(exception);
    throw new RuntimeException(exception);
  }

  @Override
  public Publisher<CoreEvent> apply(Exception exception) {
    onError(exception);
    return Mono.error(exception);
  }

  protected abstract void onError(Exception exception);

  @Override
  public Consumer<Exception> router(Function<Publisher<CoreEvent>, Publisher<CoreEvent>> publisherPostProcessor,
                                    Consumer<CoreEvent> continueCallback, Consumer<Throwable> propagateCallback) {
    final Consumer<Exception> router =
        FlowExceptionHandler.super.router(publisherPostProcessor, continueCallback, propagateCallback);

    return new ExceptionRouter() {

      @Override
      public void accept(Exception error) {
        LOGGER.debug("Routing error in '{}'...", this);

        onError(error);
        router.accept(error);
      }

      @Override
      public void dispose() {
        disposeIfNeeded(router, LOGGER);
      }
    };
  }

}
