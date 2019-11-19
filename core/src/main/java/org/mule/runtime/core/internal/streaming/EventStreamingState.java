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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventStreamingState {

  private final List<CaffeineBuffer<WeakReference<ManagedCursorProvider>>> providersOverflow = new ArrayList<>();
  private final AtomicBoolean disposed = new AtomicBoolean(false);
  private final FunctionalReadWriteLock providersLock = readWriteLock();

  private CaffeineBuffer<WeakReference<ManagedCursorProvider>> providers = new CaffeineBoundedBuffer<>();

  public ManagedCursorProvider addProvider(ManagedCursorProvider provider, StreamingGhostBuster ghostBuster) {
    WeakReference<ManagedCursorProvider> ref = ghostBuster.track(provider);
    return providersLock.withReadLock(r -> {
      if (providers.offer(ref) == FULL) {
        r.release();
        providersLock.withWriteLock(() -> {
          if (providers.offer(ref) == FULL) {
            providersOverflow.add(providers);
            providers = new CaffeineBoundedBuffer<>();
            providers.offer(ref);
          }
        });
      }

      return ref.get();
    });
  }

  public void dispose() {
    if (disposed.compareAndSet(false, true)) {
      providersLock.withWriteLock(() -> {
        dispose(providers);
        providersOverflow.forEach(this::dispose);
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
