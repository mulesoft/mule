/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import org.mule.runtime.api.streaming.object.CursorIterator;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.internal.streaming.AbstractCursorIterator;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * A {@link CursorIterator} which pulls its data from an {@link ObjectStreamBuffer}.
 *
 * @see ObjectStreamBuffer
 * @since 4.0
 */
public class BufferedCursorIterator<T> extends AbstractCursorIterator<T> {

  private final ObjectStreamBuffer<T> buffer;
  private Bucket<T> bucket = null;

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
  protected T doNext(long p) {
    Position position = buffer.toPosition(p);
    if (bucket == null || !bucket.contains(position)) {
      bucket = buffer.getBucketFor(position).orElse(bucket);
    }

    if (bucket != null) {
      return bucket.get(position.getItemIndex()).orElseThrow(NoSuchElementException::new);
    } else {
      throw new NoSuchElementException();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void release() {
    bucket = null;
  }

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
  public int getSize() {
    return buffer.getSize();
  }
}
