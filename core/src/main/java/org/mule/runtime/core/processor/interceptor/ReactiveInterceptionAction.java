/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.processor.interceptor;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.core.api.interception.DefaultInterceptionEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link InterceptionAction} that does the needed hooks with {@code Reactor} into the pipeline.
 *
 * @since 4.0
 */
class ReactiveInterceptionAction implements InterceptionAction {

  private DefaultInterceptionEvent interceptionEvent;
  private ReactiveProcessor next;

  public ReactiveInterceptionAction(DefaultInterceptionEvent interceptionEvent,
                                    ReactiveProcessor next) {
    this.interceptionEvent = interceptionEvent;
    this.next = next;
  }

  @Override
  public CompletableFuture<InterceptionEvent> proceed() {
    return just(interceptionEvent.resolve())
        .transform(next)
        .map(event -> new DefaultInterceptionEvent(event))
        .cast(InterceptionEvent.class)
        .toFuture();
  }

  @Override
  public CompletableFuture<InterceptionEvent> skip() {
    interceptionEvent.resolve();
    return completedFuture(interceptionEvent);
  }
}
