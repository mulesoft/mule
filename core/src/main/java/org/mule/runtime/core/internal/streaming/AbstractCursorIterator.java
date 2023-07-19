/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;

import java.io.IOException;

/**
 * Base class for implementations of {@link CursorIterator}.
 * <p>
 * Provides template methods and enforces default behavior.
 *
 * @since 4.0
 */
public abstract class AbstractCursorIterator<T> implements CursorIterator<T> {

  private final CursorIteratorProvider provider;
  private boolean released = false;
  private boolean closed = false;
  private long position = 0;

  /**
   * Creates a new instance
   *
   * @param provider the provider which opened this cursor
   */
  public AbstractCursorIterator(CursorIteratorProvider provider) {
    checkArgument(provider != null, "provider cannot be null");
    this.provider = provider;
  }

  protected abstract T doNext(long position);

  /**
   * {@inheritDoc}
   */
  @Override
  public final T next() {
    assertNotClosed();
    T item = doNext(position);
    position++;

    return item;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getPosition() {
    return position;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void seek(long position) throws IOException {
    assertNotClosed();
    this.position = position;
  }

  @Override
  public final void close() throws IOException {
    if (!closed) {
      closed = true;
      doClose();
    }
  }

  protected abstract void doClose() throws IOException;

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isReleased() {
    return released;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorIteratorProvider getProvider() {
    return provider;
  }

  protected void assertNotClosed() {
    checkState(!released, "Stream is closed");
  }

  /**
   * @throws UnsupportedOperationException Removing from a stream is not supported
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("Removing from a stream is not supported");
  }
}
