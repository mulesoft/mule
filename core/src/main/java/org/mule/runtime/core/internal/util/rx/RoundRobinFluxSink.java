/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

import reactor.core.Disposable;
import reactor.core.publisher.FluxSink;
import reactor.util.context.Context;

public class RoundRobinFluxSink<E> implements FluxSink<E> {

  private final List<FluxSink<E>> fluxSinks;
  private final AtomicInteger index = new AtomicInteger(0);
  // Saving update function to avoid creating the lambda every time
  private final IntUnaryOperator update;

  public RoundRobinFluxSink(int size, Supplier<FluxSink<E>> sinkFactory) {
    final List<FluxSink<E>> sinks = new ArrayList<FluxSink<E>>(size);
    for (int i = 0; i < size; i++) {
      sinks.add(sinkFactory.get());
    }
    this.fluxSinks = sinks;
    this.update = (value) -> (value + 1) % size;
  }

  private int nextIndex() {
    return index.getAndUpdate(update);
  }

  @Override
  public void complete() {
    fluxSinks.forEach(s -> s.complete());
  }

  @Override
  public Context currentContext() {
    return fluxSinks.get(0).currentContext();
  }

  @Override
  public void error(Throwable e) {
    fluxSinks.get(nextIndex()).error(e);
  }

  @Override
  public FluxSink<E> next(E t) {
    return fluxSinks.get(nextIndex()).next(t);
  }

  @Override
  public long requestedFromDownstream() {
    return fluxSinks.stream().mapToLong(s -> s.requestedFromDownstream()).sum();
  }

  @Override
  public boolean isCancelled() {
    return fluxSinks.get(0).isCancelled();
  }

  @Override
  public FluxSink<E> onRequest(LongConsumer consumer) {
    fluxSinks.forEach(s -> s.onRequest(consumer));
    return this;
  }

  @Override
  public FluxSink<E> onCancel(Disposable d) {
    fluxSinks.forEach(s -> s.onCancel(d));
    return this;
  }

  @Override
  public FluxSink<E> onDispose(Disposable d) {
    fluxSinks.forEach(s -> s.onDispose(d));
    return this;
  }

}
