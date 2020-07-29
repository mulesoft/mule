/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

import static org.mule.runtime.core.internal.streaming.CursorUtils.createKey;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;

/**
 * Tracks the active streaming resources owned by a particular event.
 *
 * @since 4.3.0
 */
public class EventStreamingState {

  private final Logger LOGGER = getLogger(EventStreamingState.class);

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
    final int key = createKey(provider);
    ManagedCursorProvider managedProvider;
    List<ManagedCursorProvider> managedCursorProviders = getOrAddManagedProviderIfKeyNotPresent(key, provider, ghostBuster);

    if (managedCursorProviders.isEmpty()) {
      // This can happen when a foreach component splits a text document using a stream.
      // Iteration N might try to manage the same root provider that was already managed in iteration N-1, but the
      // managed decorator from that previous iteration has been collected, which causes the weak reference to yield
      // a null value. In which case we simply track it again.
      synchronized (provider.getDelegate()) {
        managedCursorProviders = getOrAddManagedProviderIfKeyNotPresent(key, provider, ghostBuster);
        if (managedCursorProviders.isEmpty()) {
          // Invalidating key
          invalidateKey(key);
          managedCursorProviders = getOrAddManagedProviderIfKeyNotPresent(key, provider, ghostBuster);
        }
      }
    }
    // Try to find cursor provider
    Optional<ManagedCursorProvider> cachedManagedProvider = findCursorProvider(managedCursorProviders, provider);
    if (cachedManagedProvider.isPresent()) {
      managedProvider = cachedManagedProvider.get();
    } else {
      // Already managed cursor provider not contains current provider.
      synchronized (provider.getDelegate()) {
        // Ensure key already exists
        managedCursorProviders = getOrAddManagedProviderIfKeyNotPresent(key, provider, ghostBuster);
        if (managedCursorProviders.isEmpty()) {
          // Invalidating key
          invalidateKey(key);
          managedCursorProviders = getOrAddManagedProviderIfKeyNotPresent(key, provider, ghostBuster);
        }
        managedProvider = findCursorProvider(managedCursorProviders, provider).orElseGet(() -> {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("Already managed cursor provider with key (%s) - adding new one", key));
          }
          return addManagedCursorProvider(key, provider, ghostBuster);
        });
      }
    }

    return managedProvider;
  }

  private List<ManagedCursorProvider> getOrAddManagedProviderIfKeyNotPresent(int key, ManagedCursorProvider provider,
                                                                             StreamingGhostBuster ghostBuster) {
    return filterNotNullReferences(providers.get(key, k -> {
      List<WeakReference<ManagedCursorProvider>> references = new ArrayList<>();
      references.add(ghostBuster.track(provider));
      return references;
    }).stream());
  }

  private List<ManagedCursorProvider> filterNotNullReferences(Stream<WeakReference<ManagedCursorProvider>> references) {
    return references.filter(wr -> ofNullable(wr.get()).isPresent()).map(Reference::get).collect(toList());
  }

  private Optional<ManagedCursorProvider> findCursorProvider(List<ManagedCursorProvider> providers,
                                                             ManagedCursorProvider managedCursorProvider) {
    return providers.stream().filter(p -> p.getDelegate().equals(managedCursorProvider.getDelegate())).findFirst();
  }

  private ManagedCursorProvider addManagedCursorProvider(int key, ManagedCursorProvider provider,
                                                         StreamingGhostBuster ghostBuster) {
    List<WeakReference<ManagedCursorProvider>> weakReferences = providers.get(key, k -> {
      List<WeakReference<ManagedCursorProvider>> references = new ArrayList<>();
      references.add(ghostBuster.track(provider));
      return references;
    });

    Optional<ManagedCursorProvider> managedCursorProvider =
        findCursorProvider(filterNotNullReferences(weakReferences.stream()), provider);
    if (managedCursorProvider.isPresent()) {
      return managedCursorProvider.get();
    } else {
      weakReferences.add(ghostBuster.track(provider));
      return provider;
    }
  }

  private void invalidateKey(int key) {
    ofNullable(providers.getIfPresent(key)).ifPresent(weakReferences -> {
      weakReferences.removeIf(wr -> ofNullable(wr.get()).isPresent());
      if (weakReferences.isEmpty()) {
        providers.invalidate(key);
      }
    });
  }

  /**
   * The owning event MUST invoke this method when the event is completed
   */
  public void dispose() {
    providers.asMap().forEach((hash, weakReferences) -> {
      weakReferences.stream().forEach(weakReference -> {
        ManagedCursorProvider provider = weakReference.get();
        if (provider != null) {
          weakReference.clear();
          provider.releaseResources();
        }
      });
    });
  }
}
