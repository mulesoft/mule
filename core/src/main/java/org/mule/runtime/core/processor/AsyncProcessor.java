/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.runtime.core.util.rx.Exceptions.checkedFunction;
import static org.mule.runtime.core.util.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.core.util.rx.internal.Operators.nullSafeMap;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.exception.MessagingException;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Mono;

/**
 * Processes {@link Event}'s. Implementations that do not mutate the {@link Event} or pass it on to another MessageProcessor
 * should return the MuleEvent they receive.
 *
 * Implementations should return a {@link Publisher} implementation immediately, without blocking, and then signal the completion
 * of processing via the {@link Publisher} to it's {@link Subscriber}. Completion may with i) with an {@link Event}, ii) without
 * an {@link Event} or iii) with an error, represented using a {@link MessagingException}.
 *
 * The {@link Processor} interface is extended and a default implementation of {@link Processor#process(Event)} provided for
 * backwards compatibility only. See MULE-11250 Remove blocking process method in AsyncProcessor or implement two types of
 * MessageSource.
 *
 * @since 4.0
 */
public interface AsyncProcessor extends ReactiveProcessor {

  /**
   * Default implementation of {@link #processAsync(Event)} that delegates to {@link #process(Event)}. AsyncProcessors that
   * explicitly support async processing should override this method.
   *
   * TODO MULE-11250 Remove blocking process method in AsyncProcessor or implement two types of MessageSource
   * 
   * @param event Event to be processed
   * @return event publisher.
   */
  Publisher<Event> processAsync(Event event);
  //{
  //  return just(event).handle(nullSafeMap(checkedFunction(request -> process(request))));
  //}

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
