/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.rx.internal;

import static org.mule.runtime.core.util.rx.Exceptions.newEventDroppedException;
import org.mule.runtime.core.api.Event;

import java.util.function.BiConsumer;
import java.util.function.Function;

import reactor.core.publisher.SynchronousSink;

/**
 * Reuseable operators to be use with project reactor.
 */
public final class Operators {

  /**
   * Custom function to be used with {@link reactor.core.publisher.Flux#handle(BiConsumer)} when a map function may return
   * {@code null} and this should be interpreted as empty rather than causing an error. If null is return by the function then the
   * {@link org.mule.runtime.core.api.EventContext} is also completed.
   * 
   * @param mapper map function
   * @return custom operator {@link BiConsumer} to be used with {@link reactor.core.publisher.Flux#handle(BiConsumer)}.
   */
  public static BiConsumer<Event, SynchronousSink<Event>> nullSafeMap(Function<Event, Event> mapper) {
    return (t, sink) -> {
      if (t != null) {
        Event r = mapper.apply(t);
        if (r != null) {
          sink.next(r);
        } else {
          sink.error(newEventDroppedException(t));
        }
      }
    };
  }

}


