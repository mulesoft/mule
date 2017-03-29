/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import org.mule.runtime.api.streaming.objects.CursorIterator;
import org.mule.runtime.api.streaming.objects.CursorIteratorProvider;
import org.mule.runtime.core.internal.streaming.AbstractCursorIterator;

import java.io.IOException;

/**
 * A {@link CursorIterator} which pulls its data from an {@link ObjectStreamBuffer}.
 *
 * @see ObjectStreamBuffer
 * @since 4.0
 */
public class BufferedCursorIterator<T> extends AbstractCursorIterator<T> {

  private final ObjectStreamBuffer<T> buffer;

  public BufferedCursorIterator(ObjectStreamBuffer<T> buffer, CursorIteratorProvider provider) {
    super(provider);
    this.buffer = buffer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasNext() {
    return buffer.hasNext(getPosition());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected T doNext(long position) {
    return buffer.get(position);
  }

  /**
   * {@inheritDoc}
   * This implementation does nothing since the {@link #buffer} contains all the state.
   */
  @Override
  public void release() {}

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doClose() throws IOException {

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return buffer.size();
  }
}
