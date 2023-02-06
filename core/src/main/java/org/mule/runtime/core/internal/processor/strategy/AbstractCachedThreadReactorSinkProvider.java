/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MINUTES;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;

import java.util.ArrayList;
import java.util.List;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import reactor.core.publisher.FluxSink;

/**
 * Abstract implementation of {@link ReactorSinkProvider} that uses a cache for the {@link FluxSink}s per thread.
 */
public abstract class AbstractCachedThreadReactorSinkProvider implements ReactorSinkProvider {

  private static final int THREAD_CACHE_TIME_LIMIT_IN_MINUTES = 60;
  private static final int TRANSACTION_CACHE_TIME_LIMIT_IN_MINUTES = 10;
  private boolean sinkIndexEnabled;

  private final Cache<Thread, List<FluxSinkWrapper>> sinks =
      Caffeine.newBuilder().weakKeys()
          .removalListener((RemovalListener<Thread, List<FluxSinkWrapper>>) (String, coreEventFluxSinkList,
                                                                             removalCause) -> coreEventFluxSinkList
                                                                                 .forEach(FluxSinkWrapper::complete))
          .expireAfterAccess(THREAD_CACHE_TIME_LIMIT_IN_MINUTES, MINUTES).build();


  private final Cache<Thread, FluxSink<CoreEvent>> legacySinks =
      Caffeine.newBuilder().weakKeys()
          .removalListener((RemovalListener<Thread, FluxSink<CoreEvent>>) (thread, coreEventFluxSink,
                                                                           removalCause) -> coreEventFluxSink.complete())
          .expireAfterAccess(THREAD_CACHE_TIME_LIMIT_IN_MINUTES, MINUTES).build();

  private final Cache<Transaction, List<FluxSinkWrapper>> sinksNestedTx =
      Caffeine.newBuilder()
          .removalListener((RemovalListener<Transaction, List<FluxSinkWrapper>>) (transaction, coreEventFluxSinkList,
                                                                                  removalCause) ->

          coreEventFluxSinkList.forEach(FluxSinkWrapper::complete))
          .expireAfterAccess(TRANSACTION_CACHE_TIME_LIMIT_IN_MINUTES, MINUTES).build();

  private final Cache<Transaction, FluxSink<CoreEvent>> legacySinksNestedTx =
      Caffeine.newBuilder().weakKeys()
          .removalListener((RemovalListener<Transaction, FluxSink<CoreEvent>>) (transaction, coreEventFluxSink,
                                                                                removalCause) -> coreEventFluxSink.complete())
          .expireAfterAccess(TRANSACTION_CACHE_TIME_LIMIT_IN_MINUTES, MINUTES).build();

  public AbstractCachedThreadReactorSinkProvider() {}

  public AbstractCachedThreadReactorSinkProvider(boolean enabled) {
    this.sinkIndexEnabled = enabled;
  }

  public void dispose() {
    sinks.asMap().values().forEach(sinkList -> sinkList.forEach(FluxSinkWrapper::complete));
    legacySinks.asMap().values().forEach(FluxSink::complete);
    sinksNestedTx.asMap().values().forEach(sinkList -> sinkList.forEach(FluxSinkWrapper::complete));
    legacySinksNestedTx.asMap().values().forEach(FluxSink::complete);
  }

  protected void invalidateAll() {
    sinks.invalidateAll();
    legacySinks.invalidateAll();
    sinksNestedTx.invalidateAll();
    legacySinksNestedTx.invalidateAll();
  }

  @Override
  public FluxSink<CoreEvent> getSink() {
    TransactionCoordination txCoord = TransactionCoordination.getInstance();
    if (txCoord.runningNestedTransaction()) {
      if (sinkIndexEnabled) {
        return getNestedTxFluxSinkWrapper(txCoord);
      } else {
        return legacySinksNestedTx.get(txCoord.getTransaction(), tx -> createSink());
      }
    } else {
      if (sinkIndexEnabled) {
        return getSimpleFluxSinkWrapper();
      } else {
        return legacySinks.get(currentThread(), t -> createSink());
      }
    }
  }

  private FluxSink<CoreEvent> getNestedTxFluxSinkWrapper(TransactionCoordination txCoord) {
    List<FluxSinkWrapper> fluxSinkWrapperList = sinksNestedTx.get(txCoord.getTransaction(), parameterKey -> new ArrayList<>());

    for (FluxSinkWrapper fluxSinkWrapper : fluxSinkWrapperList) {
      if (fluxSinkWrapper.isBeingUsed()) {
        continue;
      }

      return fluxSinkWrapper;
    }

    FluxSinkWrapper fluxSinkWrapper = new FluxSinkWrapper(createSink());
    fluxSinkWrapperList.add(fluxSinkWrapper);
    return fluxSinkWrapper;
  }

  private FluxSinkWrapper getSimpleFluxSinkWrapper() {
    List<FluxSinkWrapper> fluxSinkWrapperList = sinks.get(currentThread(), parameterKey -> new ArrayList<>());

    for (FluxSinkWrapper fluxSinkWrapper : fluxSinkWrapperList) {
      if (fluxSinkWrapper.isBeingUsed()) {
        continue;
      }

      return fluxSinkWrapper;
    }

    FluxSinkWrapper fluxSinkWrapper = new FluxSinkWrapper(createSink());
    fluxSinkWrapperList.add(fluxSinkWrapper);
    return fluxSinkWrapper;
  }

  protected abstract FluxSink<CoreEvent> createSink();
}
