/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.rx;

import static reactor.core.Exceptions.unwrap;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.exception.MessagingException;

import java.util.function.BiConsumer;
import java.util.function.Function;

import reactor.core.*;
import reactor.core.Exceptions;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.SynchronousSink;

/**
 * Reuseable operators to be use with project reactor.
 */
public final class Operators {

  /**
   * Custom function to be used with {@link reactor.core.publisher.Flux#handle(BiConsumer)} when a map function may return
   * {@code null} and this should be interpreted as empty rather than causing an error.
   * 
   * @param mapper map function
   * @param <T> source type
   * @param <R> result type
   * @return custom operator {@link BiConsumer} to be used with {@link reactor.core.publisher.Flux#handle(BiConsumer)}.
   */
  public static <T, R> BiConsumer<T, SynchronousSink<R>> nullSafeMap(Function<T, R> mapper) {
    return (t, sink) -> {
      R r = mapper.apply(t);
      if (r != null) {
        sink.next(r);
      }
    };
  }

}


