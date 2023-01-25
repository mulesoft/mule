/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.mule.runtime.api.config.MuleRuntimeFeature.USE_TRANSACTION_SINK_INDEX;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;

import javax.inject.Inject;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import reactor.core.publisher.FluxSink;

/**
 * Abstract implementation of {@link ReactorSinkProvider} that uses a cache for the {@link FluxSink}s per thread.
 */
public abstract class AbstractCachedThreadReactorSinkProvider implements ReactorSinkProvider {

  @Inject
  FeatureFlaggingService featureFlaggingService;

  private static final int THREAD_CACHE_TIME_LIMIT_IN_MINUTES = 60;
  private static final int TRANSACTION_CACHE_TIME_LIMIT_IN_MINUTES = 10;

  private final Cache<String, FluxSinkWrapper> sinks =
      Caffeine.newBuilder()
          .removalListener((RemovalListener<String, FluxSink<CoreEvent>>) (String, coreEventFluxSink,
                                                                           removalCause) -> coreEventFluxSink.complete())
          .expireAfterAccess(THREAD_CACHE_TIME_LIMIT_IN_MINUTES, MINUTES).build();
  private final Cache<Transaction, FluxSink<CoreEvent>> sinksNestedTx =
      Caffeine.newBuilder().weakKeys()
          .removalListener((RemovalListener<Transaction, FluxSink<CoreEvent>>) (transaction, coreEventFluxSink,
                                                                                removalCause) -> coreEventFluxSink.complete())
          .expireAfterAccess(TRANSACTION_CACHE_TIME_LIMIT_IN_MINUTES, MINUTES).build();

  public void dispose() {
    sinks.asMap().values().forEach(FluxSink::complete);
    sinksNestedTx.asMap().values().forEach(FluxSink::complete);
  }

  protected void invalidateAll() {
    sinks.invalidateAll();
    sinksNestedTx.invalidateAll();
  }

  @Override
  public FluxSink<CoreEvent> getSink() {
    TransactionCoordination txCoord = TransactionCoordination.getInstance();
    if (txCoord.runningNestedTransaction()) {
      return sinksNestedTx.get(txCoord.getTransaction(), tx -> createSink());
    } else {
      if (featureFlaggingService.isEnabled(USE_TRANSACTION_SINK_INDEX)) {
        int index = 0;
        while (true) {
          FluxSinkWrapper fluxSinkWrapper =
              sinks.get(currentThread().getId() + "-" + index, t -> new FluxSinkWrapper(createSink()));
          if (fluxSinkWrapper.isBeingUsed()) {
            index++;
            continue;
          }
          return fluxSinkWrapper;
        }
      } else {
        return sinks.get(String.valueOf(currentThread()), t -> (FluxSinkWrapper) createSink());
      }
    }
  }

  protected abstract FluxSink<CoreEvent> createSink();
}
