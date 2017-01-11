/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.processor.interceptor;

import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import java.util.concurrent.CompletableFuture;

/**
 * TODO
 */
public class ReactiveInterceptionAction implements InterceptionAction {

  private CompletableFuture<Event> future;
  private Event event;
  private ReactiveProcessor next;

  public ReactiveInterceptionAction(Event event, ReactiveProcessor next) {
    this.event = event;
    this.next = next;
  }

  @Override
  public void proceed() {
    this.future = just(event).transform(next).toFuture();
  }

  public CompletableFuture<Event> getFuture() {
    return future;
  }
}
