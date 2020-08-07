/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.slf4j.LoggerFactory.getLogger;
import static org.mule.runtime.core.internal.streaming.CursorUtils.unwrap;

import java.lang.ref.WeakReference;
import java.util.Optional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.mule.runtime.api.streaming.CursorProvider;
import org.slf4j.Logger;

/**
 * Tracks the active streaming resources owned by a particular event.
 *
 * @since 4.3.0
 */
public class EventStreamingState {

  private final static Logger LOGGER = getLogger(EventStreamingState.class);

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
      synchronized (unwrap(provider)) {
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
    Optional<WeakReference<ManagedCursorProvider>> alreadyManagedOpt = findAlreadyManagedProvider(id, provider);
    if (alreadyManagedOpt.isPresent()) {
      ManagedCursorProvider alreadyManagedCursorProvider = alreadyManagedOpt.get().get();
      CursorProvider innerDelegate = unwrap(provider);
      CursorProvider delegate = unwrap(alreadyManagedCursorProvider);
      LOGGER.warn("Already managed provider: [" + provider.getId() + "], alreadyManagedCursorProvider: ["
          + alreadyManagedCursorProvider.getId() + "]. InnerDelegate: " + System.identityHashCode(innerDelegate) + " delegate: "
          + System.identityHashCode(delegate) + "]");
    }
    return providers.get(id, k -> ghostBuster.track(provider)).get();
  }

  private Optional<WeakReference<ManagedCursorProvider>> findAlreadyManagedProvider(int id, ManagedCursorProvider provider) {
    return providers.asMap().values().stream().filter(wr -> {
      ManagedCursorProvider alreadyManagedCursorProvider = wr.get();
      if (alreadyManagedCursorProvider != null && alreadyManagedCursorProvider.getId() != id) {
        CursorProvider innerDelegate = unwrap(provider);
        CursorProvider delegate = unwrap(alreadyManagedCursorProvider);
        return innerDelegate.equals(delegate);
      } else {
        return false;
      }
    }).findFirst();
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
