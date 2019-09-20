/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.internal.streaming.CursorProviderJanitor;
import org.mule.runtime.core.internal.streaming.StreamingGhostBuster;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * A decorator which allows performing management tasks over a {@link CursorStream}
 *
 * @since 4.1.6
 */
class ManagedCursorIterator<T> implements CursorIterator<T> {

  private ManagedCursorIteratorProvider managedCursorIteratorProvider;
  private CursorIteratorProvider exposedProvider;
  private final CursorIterator<T> delegate;
  private final CursorProviderJanitor janitor;

  /**
   * Creates a new instance. Notice that it receives a {@code managedCursorProvider} so that a hard reference is kept
   * during the lifespan of this cursor. This prevents the {@link StreamingGhostBuster} from closing the provider in corner
   * cases in which this cursor is still referenced but the provider is not.
   *
   * @param managedCursorIteratorProvider the managed provider that opened this cursor
   * @param delegate                      the delegate cursor
   * @param janitor                       the cursor's janitor object
   */
  ManagedCursorIterator(ManagedCursorIteratorProvider managedCursorIteratorProvider,
                        CursorIterator<T> delegate,
                        CursorProviderJanitor janitor) {
    this.managedCursorIteratorProvider = managedCursorIteratorProvider;
    exposedProvider = managedCursorIteratorProvider;
    this.delegate = delegate;
    this.janitor = janitor;
  }

  @Override
  public void close() throws IOException {
    try {
      delegate.close();
    } finally {
      if (managedCursorIteratorProvider != null) {
        exposedProvider = (CursorIteratorProvider) managedCursorIteratorProvider.getDelegate();
        managedCursorIteratorProvider = null;
      }
      janitor.releaseCursor(delegate);
    }
  }

  @Override
  public boolean hasNext() {
    return delegate.hasNext();
  }

  @Override
  public T next() {
    return delegate.next();
  }

  @Override
  public void remove() {
    delegate.remove();
  }

  @Override
  public void forEachRemaining(Consumer<? super T> action) {
    delegate.forEachRemaining(action);
  }

  @Override
  public long getPosition() {
    return delegate.getPosition();
  }

  @Override
  public void seek(long position) throws IOException {
    delegate.seek(position);
  }

  @Override
  public void release() {
    delegate.release();
  }

  @Override
  public boolean isReleased() {
    return delegate.isReleased();
  }

  @Override
  public CursorProvider getProvider() {
    return exposedProvider;
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }
}
