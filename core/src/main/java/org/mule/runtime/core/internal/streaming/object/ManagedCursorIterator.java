/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.core.internal.streaming.CursorProviderJanitor;

import java.io.IOException;
import java.util.function.Consumer;

class ManagedCursorIterator<T> implements CursorIterator<T> {

  private final CursorIterator<T> delegate;
  private final CursorProviderJanitor janitor;

  ManagedCursorIterator(CursorIterator<T> delegate, CursorProviderJanitor janitor) {
    this.delegate = delegate;
    this.janitor = janitor;
  }

  @Override
  public void close() throws IOException {
    try {
      delegate.close();
    } finally {
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
    return delegate.getProvider();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }
}
