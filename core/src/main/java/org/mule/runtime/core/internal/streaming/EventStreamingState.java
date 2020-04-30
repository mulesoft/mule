/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import java.lang.ref.WeakReference;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Tracks the active streaming resources owned by a particular event.
 *
 * @since 4.3.0
 */
public class EventStreamingState {

  private final Cache<Integer, WeakReference<ManagedCursorProvider>> providers = Caffeine.newBuilder().build();

  /**
   * Registers the given {@code provider} as one associated to the owning event.
   * <p>
   * Consumers of this method must discard the passed {@code provider} and used the returned one instead
   *
   * @param provider    a {@link ManagedCursorProvider}
   * @param ghostBuster the {@link StreamingGhostBuster} used to do early reclamation of the {@code provider}
   * @return the {@link ManagedCursorProvider} that must continue to be used
   */
  public ManagedCursorProvider addProvider(ManagedCursorProvider provider, StreamingGhostBuster ghostBuster) {
    final int id = provider.getId();
    ManagedCursorProvider managedProvider = getOrAddManagedProvider(id, provider, ghostBuster);

    // This can happen when a foreach component splits a text document using a stream.
    // Iteration N might try to manage the same root provider that was already managed in iteration N-1, but the
    // managed decorator from that previous iteration has been collected, which causes the weak reference to yield
    // a null value. In which case we simply track it again.
    if (managedProvider == null) {
      synchronized (provider.getDelegate()) {
        managedProvider = getOrAddManagedProvider(id, provider, ghostBuster);
        if (managedProvider == null) {
          providers.invalidate(id);
          managedProvider = getOrAddManagedProvider(id, provider, ghostBuster);
        }
      }
    }

    return managedProvider;
  }

  private ManagedCursorProvider getOrAddManagedProvider(int id,
                                                        ManagedCursorProvider provider,
                                                        StreamingGhostBuster ghostBuster) {
    return providers.get(id, k -> ghostBuster.track(provider)).get();
  }

  /**
   * The owning event MUST invoke this method when the event is completed
   */
  public void dispose() {
    providers.asMap().forEach((hash, weakReference) -> {
      ManagedCursorProvider provider = weakReference.get();
      if (provider != null) {
        weakReference.clear();
        provider.releaseResources();
      }
    });
  }
}
