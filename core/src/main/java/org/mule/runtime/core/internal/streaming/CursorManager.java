/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static java.util.Collections.newSetFromMap;
import static java.util.Collections.synchronizedSet;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.internal.streaming.bytes.ManagedCursorStreamProvider;
import org.mule.runtime.core.internal.streaming.object.ManagedCursorIteratorProvider;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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
   * @param provider the provider to be tracked
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

    registry.get(ownerContext).addProvider(managedProvider);

    return managedProvider;
  }

  private void terminated(BaseEventContext context) {
    registry.invalidate(context);
  }

  private void hookEventTermination(BaseEventContext ownerContext) {
    ownerContext.onTerminated((response, throwable) -> terminated(ownerContext));
  }

  private class EventStreamingState {

    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final Set<WeakReference<ManagedCursorProvider>> providers =
        synchronizedSet(newSetFromMap(new WeakHashMap<WeakReference<ManagedCursorProvider>, Boolean>()));

    private void addProvider(ManagedCursorProvider provider) {
      providers.add(ghostBuster.track(provider));
    }

    private void dispose() {
      if (disposed.compareAndSet(false, true)) {
        providers.forEach(weakReference -> {
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
