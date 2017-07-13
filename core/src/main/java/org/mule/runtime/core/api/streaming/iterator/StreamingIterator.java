/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.streaming.iterator;

import org.mule.runtime.api.streaming.HasSize;

import java.io.Closeable;
import java.util.Iterator;

/**
 * {@link Iterator} that also extends {@link Closeable} and {@link HasSize}.
 *
 * The {@link Iterator#remove()} operation is not allowed for this iterator.
 * 
 * @param <T> the type of elements returned by this iterator
 */
public interface StreamingIterator<T> extends Iterator<T>, Closeable, HasSize {

  /**
   * Not allowed on this implementations
   *
   * @throws UnsupportedOperationException
   */
  @Override
  default void remove() {
    throw new UnsupportedOperationException();
  }
}
