/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.rx;

import org.mule.runtime.core.internal.exception.MessagingException;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * Provide a single API to interact with sinks for both {@link Mono}s and {@link Flux}es.
 * <p>
 * This is helpful in specific cases where a {@link Publisher} must be build with the same operators regardless of whether the
 * base is a {@link Mono} or a {@link Flux}.
 *
 * @param <T> The type of values to provide to the sink
 *
 * @since 4.2
 */
public interface SinkRecorderToReactorSinkAdapter<T> {

  /**
   * See {@link MonoSink#success()} and {@link FluxSink#next(Object)}.
   */
  void next();

  /**
   * See {@link MonoSink#success(Object)} and {@link FluxSink#next(Object)}.
   */
  void next(T response);

  /**
   * See {@link MonoSink#error(Throwable)} and {@link FluxSink#error(Throwable)}.
   */
  void error(MessagingException error);

}
