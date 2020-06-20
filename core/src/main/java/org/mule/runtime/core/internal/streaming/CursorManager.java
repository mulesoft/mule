/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static java.lang.System.identityHashCode;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.internal.streaming.bytes.ManagedCursorStreamProvider;
import org.mule.runtime.core.internal.streaming.object.ManagedCursorIteratorProvider;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

/**
 * Keeps track of active {@link Cursor cursors} and their {@link CursorProvider providers}
 *
 * @since 4.0
 */
public class CursorManager {

  private final LoadingCache<BaseEventContext, EventStreamingState> registry =
      Caffeine.newBuilder()
          .removalListener((context, state, cause) -> ((EventStreamingState) state).dispose())
          .build(context -> {
            hookEventTermination(context);
            return new EventStreamingState();
          });

  private final MutableStreamingStatistics statistics;
  private final StreamingGhostBuster ghostBuster;

  /**
   * Creates a new instance
   *
   * @param statistics statistics which values should be kept updated
   */
  public CursorManager(MutableStreamingStatistics statistics, StreamingGhostBuster ghostBuster) {
    this.statistics = statistics;
    this.ghostBuster = ghostBuster;
  }

  /**
   * Becomes aware of the given {@code provider} and returns a replacement provider which is managed by the runtime, allowing for
   * automatic resource handling
   *
   * @param provider     the provider to be tracked
   * @param ownerContext the root context of the event that created the provider
   * @return a {@link CursorProvider}
   */
  public CursorProvider manage(CursorProvider provider, BaseEventContext ownerContext) {
    ManagedCursorProvider managedProvider;
    if (provider instanceof CursorStreamProvider) {
      managedProvider = new ManagedCursorStreamProvider(provider, statistics);
    } else if (provider instanceof CursorIteratorProvider) {
      managedProvider = new ManagedCursorIteratorProvider(provider, statistics);
    } else {
      throw new MuleRuntimeException(createStaticMessage("Unknown cursor provider type: " + provider.getClass().getName()));
    }

    return registry.get(ownerContext).addProvider(managedProvider);
  }

  private void terminated(BaseEventContext context) {
    registry.invalidate(context);
  }

  private void hookEventTermination(BaseEventContext ownerContext) {
    ownerContext.onTerminated((response, throwable) -> terminated(ownerContext));
  }

  private class EventStreamingState {

    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final Cache<Integer, WeakReference<ManagedCursorProvider>> providers = Caffeine.newBuilder().build();

    private ManagedCursorProvider addProvider(ManagedCursorProvider provider) {
      final int hash = identityHashCode(provider.getDelegate());
      ManagedCursorProvider managedProvider = getOrAddManagedProvider(provider, hash);

      // This can happen when a foreach component splits a text document using a stream.
      // Iteration N might try to manage the same root provider that was already managed in iteration N-1, but the
      // managed decorator from that previous iteration has been collected, which causes the weak reference to yield
      // a null value. In which case we simply track it again.
      if (managedProvider == null) {
        synchronized (provider.getDelegate()) {
          managedProvider = getOrAddManagedProvider(provider, hash);
          if (managedProvider == null) {
            providers.invalidate(hash);
            managedProvider = getOrAddManagedProvider(provider, hash);
          }
        }
      }

      return managedProvider;
    }

    private ManagedCursorProvider getOrAddManagedProvider(ManagedCursorProvider provider, int hash) {
      return providers.get(hash, k -> ghostBuster.track(provider)).get();
    }

    private void dispose() {
      if (disposed.compareAndSet(false, true)) {
        providers.asMap().forEach((hash, weakReference) -> {
          ManagedCursorProvider provider = weakReference.get();
          if (provider != null) {
            weakReference.clear();
            provider.releaseResources();
          }
        });
      }
    }
  }
}
