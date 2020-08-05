/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.core.internal.streaming.CursorUtils.managedCursorProviderKey;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Tracks the active streaming resources owned by a particular event.
 *
 * @since 4.3.0
 */
public class EventStreamingState {

  private final Cache<Integer, List<WeakReference<ManagedCursorProvider>>> providers = Caffeine.newBuilder().build();

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
    final int key = managedCursorProviderKey(provider);
    ManagedCursorProvider managedProvider = getOrAddManagedProvider(key, provider, ghostBuster);
    return managedProvider;
  }

  private ManagedCursorProvider getOrAddManagedProvider(int key,
                                                        ManagedCursorProvider provider,
                                                        StreamingGhostBuster ghostBuster) {

    List<WeakReference<ManagedCursorProvider>> managedList = providers.get(key, k -> new ArrayList<>());

    ManagedCursorProvider managed = null;

    synchronized (managedList) {
      if (managedList.isEmpty()) {
        WeakReference<ManagedCursorProvider> track = ghostBuster.track(provider);
        managedList.add(track);
        managed = track.get();
      } else {
        // Try to find already managed cursor provider
        for (WeakReference<ManagedCursorProvider> weakReference : managedList) {
          ManagedCursorProvider ref = weakReference.get();
          if (ref != null) {
            // Found already managed cursor provider
            if (ref.getDelegate() == provider.getDelegate()) {
              managed = ref;
              break;
            }
          }
        }
        // This can happen on 2 scenarios:

        // 1- when a foreach component splits a text document using a stream.
        // Iteration N might try to manage the same root provider that was already managed in iteration N-1, but the
        // managed decorator from that previous iteration has been collected, which causes the weak reference to yield
        // a null value. In which case we simply track it again.
        // 2- Found key collision, different root provider generate same key. In which case we simply track it again.
        if (managed == null) {
          // Cleanup collected weak reference
          managedList.removeIf(track -> track.get() == null);

          // Add new entry
          WeakReference<ManagedCursorProvider> track = ghostBuster.track(provider);
          managedList.add(track);
          managed = track.get();
        }
      }
    }

    return managed;
  }

  /**
   * The owning event MUST invoke this method when the event is completed
   */
  public void dispose() {
    providers.asMap().forEach((hash, managedList) -> {
      for (WeakReference<ManagedCursorProvider> weakReference : managedList) {
        ManagedCursorProvider provider = weakReference.get();
        if (provider != null) {
          weakReference.clear();
          provider.releaseResources();
        }
      }
    });
  }
}
