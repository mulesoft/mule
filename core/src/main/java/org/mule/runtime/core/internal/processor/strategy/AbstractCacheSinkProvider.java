/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MINUTES;

public abstract class AbstractCacheSinkProvider {

  private static final int THREAD_CACHE_TIME_LIMIT_IN_MINUTES = 60;
  private static final int TRANSACTION_CACHE_TIME_LIMIT_IN_MINUTES = 10;

  private final Cache<Thread, FluxSink<CoreEvent>> sinks =
      Caffeine.newBuilder().weakKeys()
          .removalListener((RemovalListener<Thread, FluxSink<CoreEvent>>) (thread, coreEventFluxSink,
                                                                           removalCause) -> coreEventFluxSink.complete())
          .expireAfterAccess(THREAD_CACHE_TIME_LIMIT_IN_MINUTES, MINUTES).build();
  private final Cache<Transaction, FluxSink<CoreEvent>> sinksNestedTx =
      Caffeine.newBuilder().weakKeys()
          .removalListener((RemovalListener<Transaction, FluxSink<CoreEvent>>) (transaction, coreEventFluxSink,
                                                                                removalCause) -> coreEventFluxSink.complete())
          .expireAfterAccess(TRANSACTION_CACHE_TIME_LIMIT_IN_MINUTES, MINUTES).build();

  public abstract FluxSink<CoreEvent> getSink(AtomicLong disposableSinks, FlowConstruct flowConstruct);

  public void dispose() {
    sinks.asMap().values().forEach(sink -> sink.complete());
    sinksNestedTx.asMap().values().forEach(sink -> sink.complete());
  }

  public void invalidateAll() {
    sinks.invalidateAll();
    sinksNestedTx.invalidateAll();
  }

  public void accept(StreamPerThreadSink streamPerThreadSink, CoreEvent event) {
    TransactionCoordination txCoord = TransactionCoordination.getInstance();
    if (txCoord.runningNestedTransaction()) {
      sinksNestedTx.get(txCoord.getTransaction(), tx -> streamPerThreadSink.createSink()).next(event);
    } else {
      sinks.get(currentThread(), t -> streamPerThreadSink.createSink()).next(event);
    }
  };
}
