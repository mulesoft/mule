/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.mule.runtime.api.lifecycle.Disposable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

import reactor.core.publisher.FluxSink;

/**
 * Provides a round-robin access to the internally kept {@link FluxSink}s built with factory function.
 *
 * @param <T> the value type
 *
 * @since 4.2
 */
public class RoundRobinFluxSinkSupplier<T> implements Supplier<FluxSink<T>>, Disposable {

  private final List<FluxSink<T>> fluxSinks;
  private final AtomicInteger index = new AtomicInteger(0);
  // Saving update function to avoid creating the lambda every time
  private final IntUnaryOperator update;
  private final Supplier<FluxSink<T>> sinkFactory;
  private final Cache<Thread, FluxSink<T>> sinks = Caffeine.newBuilder().weakKeys().build();

  public RoundRobinFluxSinkSupplier(int size, Supplier<FluxSink<T>> sinkFactory) {
    final List<FluxSink<T>> sinks = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      sinks.add(sinkFactory.get());
    }
    this.fluxSinks = sinks;
    this.update = (value) -> (value + 1) % size;
    this.sinkFactory = sinkFactory;
  }

  private int nextIndex() {
    return index.getAndUpdate(update);
  }

  @Override
  public void dispose() {
    fluxSinks.forEach(FluxSink::complete);
    sinks.asMap().forEach((t, sink) -> sink.complete());
  }

  @Override
  public FluxSink<T> get() {
    // Explanation here
    if (isTransactionActive()) {
      return sinks.get(currentThread(), t -> sinkFactory.get());
    } else {
      return fluxSinks.get(nextIndex());
    }
  }

}
