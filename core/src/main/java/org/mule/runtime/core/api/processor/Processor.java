/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import static org.mule.runtime.core.api.rx.Exceptions.checkedFunction;
import static org.mule.runtime.core.internal.util.rx.Operators.nullSafeMap;
import static reactor.core.publisher.Flux.from;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;

import org.reactivestreams.Publisher;

/**
 * Processes {@link Event}'s. Implementations that do not mutate the {@link Event} or pass it on to another MessageProcessor
 * should return the MuleEvent they receive.
 *
 * From 4.0 this interface also extends {@link ReactiveProcessor} and implementations of this interface can be used in
 * {@link Event} stream processing via the default implementation of {@link #apply(Publisher)} that performs a map function on the
 * stream using the result of the invocation of the blocking {@link #process(Event)} method. Using this approach simple processor
 * implementations that don't block or perform blocking IO can continue to implement {@link Processor} and require no changes.
 * 
 * @since 3.0
 */
public interface Processor extends ReactiveProcessor {

  /**
   * Invokes the MessageProcessor.
   * 
   * @param event MuleEvent to be processed
   * @return optional response MuleEvent
   * @throws MuleException
   */
  Event process(Event event) throws MuleException;

  /**
   * Applies a {@link Publisher<Event>} function transforming a stream of {@link Event}'s.
   * <p>
   * The default implementation delegates to {@link #process(Event)} and will:
   * <ol>
   * <li>propagate any exception thrown</li>
   * <li>drop events if invocation of {@link #process(Event)} returns null.</li>
   * </ol>
   *
   * @param publisher the event stream to transform
   * @return the transformed event stream
   */
  @Override
  default Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher).handle(nullSafeMap(checkedFunction(event -> process(event))));
  }

}
