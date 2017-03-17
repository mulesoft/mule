/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import org.mule.runtime.api.streaming.objects.CursorIterator;
import org.mule.runtime.api.streaming.objects.CursorIteratorProvider;

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
  private boolean fullyConsumed = false;
  private boolean released = false;
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
    assertNotDisposed();
    setFullyConsumed(false);
    this.position = position;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isFullyConsumed() {
    return fullyConsumed;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canBeReleased() {
    return fullyConsumed || released;
  }

  protected void assertNotDisposed() {
    checkState(!released, "Stream is closed");
  }

  protected void setFullyConsumed(boolean fullyConsumed) {
    this.fullyConsumed = fullyConsumed;
  }

}
