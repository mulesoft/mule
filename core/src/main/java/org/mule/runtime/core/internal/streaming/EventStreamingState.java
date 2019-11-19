/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.core.api.util.concurrent.FunctionalReadWriteLock.readWriteLock;
import static org.mule.runtime.core.internal.concurrent.CaffeineBuffer.FULL;

import org.mule.runtime.core.api.util.concurrent.FunctionalReadWriteLock;
import org.mule.runtime.core.internal.concurrent.CaffeineBoundedBuffer;
import org.mule.runtime.core.internal.concurrent.CaffeineBuffer;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks the active streaming resources owned by a particular event.
 *
 * @since 4.3.0
 */
public class EventStreamingState {

  private final AtomicBoolean disposed = new AtomicBoolean(false);
  private final FunctionalReadWriteLock providersLock = readWriteLock();

  private CaffeineBuffer<WeakReference<ManagedCursorProvider>> providers = new CaffeineBoundedBuffer<>();
  private List<CaffeineBuffer<WeakReference<ManagedCursorProvider>>> providersOverflow = null;

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
    WeakReference<ManagedCursorProvider> ref = ghostBuster.track(provider);
    return providersLock.withReadLock(r -> {
      if (providers.offer(ref) == FULL) {
        r.release();
        providersLock.withWriteLock(() -> {
          if (providers.offer(ref) == FULL) {
            if (providersOverflow == null) {
              providersOverflow = new LinkedList<>();
            }
            providersOverflow.add(providers);
            providers = new CaffeineBoundedBuffer<>();
            providers.offer(ref);
          }
        });
      }

      return ref.get();
    });
  }

  /**
   * The owning event MUST invoke this method when the event is completed
   */
  public void dispose() {
    if (disposed.compareAndSet(false, true)) {
      providersLock.withWriteLock(() -> {
        dispose(providers);
        if (providersOverflow != null) {
          providersOverflow.forEach(this::dispose);
        }
      });
    }
  }

  private void dispose(CaffeineBuffer<WeakReference<ManagedCursorProvider>> buffer) {
    buffer.drainTo(weakReference -> {
      ManagedCursorProvider provider = weakReference.get();
      if (provider != null) {
        weakReference.clear();
        provider.releaseResources();
      }
    });
  }
}
