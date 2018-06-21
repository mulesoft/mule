/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.core.api.event.CoreEvent;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Base implementation for exception handlers.
 *
 * @since 4.2
 */
public abstract class BaseExceptionHandler implements FlowExceptionHandler {

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

}
