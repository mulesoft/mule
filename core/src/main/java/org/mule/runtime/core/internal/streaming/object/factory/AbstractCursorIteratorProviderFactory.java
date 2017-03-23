/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object.factory;

import static org.mule.runtime.core.api.functional.Either.left;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.api.streaming.objects.CursorIterator;
import org.mule.runtime.api.streaming.objects.CursorIteratorProvider;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.internal.streaming.CursorManager;
import org.mule.runtime.core.internal.streaming.CursorProviderHandle;
import org.mule.runtime.core.internal.streaming.ManagedCursorProvider;
import org.mule.runtime.core.internal.streaming.object.iterator.StreamingIterator;
import org.mule.runtime.core.streaming.objects.CursorIteratorProviderFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Base implementation of {@link CursorIteratorProviderFactory} which contains all the base behaviour and template
 * methods.
 * <p>
 * It interacts with the {@link CursorManager} in order to track all allocated resources and make
 * sure they're properly disposed of once they're no longer necessary.
 *
 * @since 4.0
 */
public abstract class AbstractCursorIteratorProviderFactory implements CursorIteratorProviderFactory {

  private final CursorManager cursorManager;

  /**
   * Creates a new instance
   *
   * @param cursorManager the manager which will track the produced providers.
   */
  protected AbstractCursorIteratorProviderFactory(CursorManager cursorManager) {
    this.cursorManager = cursorManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Either<CursorIteratorProvider, Iterator> of(Event event, Iterator iterator) {
    if (iterator instanceof CursorIterator) {
      return left((CursorIteratorProvider) ((CursorIterator) iterator).getProvider());
    }

    Either<CursorIteratorProvider, Iterator> value = resolve(iterator, event);
    return value.mapLeft(provider -> {
      CursorProviderHandle handle = cursorManager.track(provider, event);
      return new ManagedCursorIteratorProvider(handle, cursorManager);
    });
  }

  /**
   * Implementations should use this method to actually create the output value
   *
   * @param iterator the streaming iterator
   * @param event    the event on which streaming is happening
   * @return
   */
  protected abstract Either<CursorIteratorProvider, Iterator> resolve(Iterator iterator, Event event);

  /**
   * {@inheritDoc}
   *
   * @return {@code true} if the {@code value} is a {@link StreamingIterator}
   */
  @Override
  public boolean accepts(Object value) {
    return value instanceof StreamingIterator;
  }

  private class ManagedCursorIteratorProvider extends ManagedCursorProvider<CursorIterator> implements CursorIteratorProvider {

    public ManagedCursorIteratorProvider(CursorProviderHandle cursorProviderHandle, CursorManager cursorManager) {
      super(cursorProviderHandle, cursorManager);
    }

    @Override
    protected CursorIterator managedCursor(CursorIterator cursor, CursorProviderHandle handle) {
      return new ManagedCursorIterator(cursor, handle);
    }
  }

  private class ManagedCursorIterator<T> implements CursorIterator<T> {

    private final CursorIterator<T> delegate;
    private final CursorProviderHandle cursorProviderHandle;

    private ManagedCursorIterator(CursorIterator<T> delegate, CursorProviderHandle cursorProviderHandle) {
      this.delegate = delegate;
      this.cursorProviderHandle = cursorProviderHandle;
    }

    @Override
    public void close() throws IOException {
      try {
        delegate.close();
      } finally {
        cursorManager.onClose(delegate, cursorProviderHandle);
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
    public int size() {
      return delegate.size();
    }
  }
}
