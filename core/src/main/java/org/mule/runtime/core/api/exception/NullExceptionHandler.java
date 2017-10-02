/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.exception;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

/**
 * Null {@link FlowExceptionHandler} which can be used to configure a {@link MessageProcessorChain} to not handle errors.
 *
 * @since 4.0
 */
public final class NullExceptionHandler implements FlowExceptionHandler {

  private static final NullExceptionHandler INSTANCE = new NullExceptionHandler();

  private NullExceptionHandler() {}

  public static FlowExceptionHandler getInstance() {
    return INSTANCE;
  }

  @Override
  public CoreEvent handleException(Exception exception, CoreEvent event) {
    throw new RuntimeException(exception);
  }

  @Override
  public Publisher<CoreEvent> apply(Exception exception) {
    return Mono.error(exception);
  }
}
