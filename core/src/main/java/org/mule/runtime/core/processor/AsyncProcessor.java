/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static reactor.core.publisher.Flux.from;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.exception.MessagingException;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * Processes {@link Event}'s. Implementations that do not mutate the {@link Event} or pass it on to another MessageProcessor
 * should return the MuleEvent they receive.
 *
 * Implementations should return a {@link Publisher} immediately without blocking, and then signal the completion of processing
 * via the {@link Publisher} to it's {@link Subscriber}. Completion may be with i) with an {@link Event}, ii) without an
 * {@link Event} or iii) with an error, represented by a {@link MessagingException}.
 *
 * @since 4.0
 */
public interface AsyncProcessor extends ReactiveProcessor {

  /**
   * Invokes the processor asynchronously and completes the returned {@link Publisher} on completion of processing.
   *
   * @param event Event to be processed
   * @return result publisher.
   */
  Publisher<Event> processAsync(Event event);

  /**
   * Applies a {@link Publisher<Event>} function transforming a stream of {@link Event}'s.
   * <p>
   * The default implementation delegates to {@link #processAsync(Event)}.
   *
   * @param publisher the event stream to transform
   * @return the transformed event stream
   */
  default Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher).flatMap(event -> processAsync(event));
  }

}
