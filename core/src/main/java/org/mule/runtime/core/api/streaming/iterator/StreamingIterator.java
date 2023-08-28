/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.streaming.iterator;

import org.mule.api.annotation.NoImplement;
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
@NoImplement
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
