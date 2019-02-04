/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.internal.streaming.ManagedCursorProvider;
import org.mule.runtime.core.internal.streaming.MutableStreamingStatistics;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * {@link ManagedCursorProvider} implementation for {@link CursorIteratorProvider} instances
 *
 * @since 4.0
 */
public class ManagedCursorIteratorProvider extends ManagedCursorProvider<CursorIterator> implements CursorIteratorProvider {

  /**
   * {@inheritDoc}
   */
  public ManagedCursorIteratorProvider(CursorProvider<CursorIterator> delegate, MutableStreamingStatistics statistics) {
    super(delegate, statistics);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected CursorIterator managedCursor(CursorIterator cursor) {
    return new ManagedCursorIterator(cursor);
  }

  private class ManagedCursorIterator<T> implements CursorIterator<T> {

    private final CursorIterator<T> delegate;

    private ManagedCursorIterator(CursorIterator<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public void close() throws IOException {
      try {
        delegate.close();
      } finally {
        ManagedCursorIteratorProvider.this.onClose(this);
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
      return delegate.getProvider();
    }

    @Override
    public int getSize() {
      return delegate.getSize();
    }
  }
}
