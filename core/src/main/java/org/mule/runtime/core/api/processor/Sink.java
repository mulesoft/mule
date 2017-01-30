/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Used to dispatch {@link Event}'s asynchronously for processing. The result of asynchronous processing can be obtained by
 * subscribing to the {@link Event}'s {@link EventContext}.
 * 
 * All Sinks should support concurrent calls from multiple publishers and it is then up to each implementation to determine how to
 * handle this, i.e. i) by continuing in the callee thread, ii) serializing all events to a single thread or iii) use a
 * ring-buffer to de-multiplex requests and then handle them with 1..n subscribers.
 *
 * @since 4.0
 */
public interface Sink extends Consumer<Event> {

  /**
   * Submit the given {@link Event} for processing without a timeout. If the {@link Event} can't not be processed immediately due
   * to back-pressure then this method will block until in can be processed.
   *
   * @param event the {@link Event} to dispatch for processing
   */
  @Override
  void accept(Event event);

}
