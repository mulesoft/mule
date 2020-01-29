/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;

import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;

import reactor.core.publisher.FluxSink;

/**
 * Provides a unique {@link FluxSink} for each Thread in transactional context. In case of non-transactional context,
 * it delegates the request.
 *
 * @param <T> the value type
 *
 * @since 4.3
 */
public class TransactionAwareFluxSinkSupplier<T> implements FluxSinkSupplier<T> {

  private final Supplier<FluxSink<T>> newSinkFactory;
  private final FluxSinkSupplier<T> delegate;
  private final Cache<Thread, FluxSink<T>> sinks = Caffeine.newBuilder()
      .weakKeys()
      .removalListener((Thread key, FluxSink<T> value, RemovalCause cause) -> value.complete())
      .build();

  public TransactionAwareFluxSinkSupplier(Supplier<FluxSink<T>> sinkFactory, FluxSinkSupplier<T> delegate) {
    this.newSinkFactory = sinkFactory;
    this.delegate = delegate;
  }

  @Override
  public FluxSink<T> get() {
    // In case of tx we need to ensure that in use of the delegate supplier there are no 2 threads trying to use the
    // same sink. This could cause a race condition in which the second thread simply queues the event in the busy sink.
    // So, this thread will not unbind the tx (causing an error next time it tries to bind one), and the first one will
    // then process the queued event without having the tx bound (so it will process as if it wasn't a tx in the
    // beginning).
    if (isTransactionActive()) {
      return sinks.get(currentThread(), t -> newSinkFactory.get());
    } else {
      return delegate.get();
    }
  }

  @Override
  public void dispose() {
    delegate.dispose();
    sinks.invalidateAll();
  }
}
