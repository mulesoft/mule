/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.streaming.iterator.StreamingIterator;

import java.io.IOException;
import java.util.function.Function;

/**
 * Decorates an {@link StreamingIterator} of elements of random types using a {@link Function} which guarantees
 * that the items are always surfaced as a {@link Message}
 *
 * This allows to avoid preemptive transformations of an entire dataset
 *
 * @since 4.4.0
 */
public final class TransformingStreamingIterator<T> extends TransformingIterator<T> implements StreamingIterator<T> {

  public TransformingStreamingIterator(StreamingIterator<?> delegate, Function<Object, T> transformer) {
    super(delegate, transformer);
  }

  @Override
  public int getSize() {
    return ((StreamingIterator) delegate).getSize();
  }

  @Override
  public void close() throws IOException {
    ((StreamingIterator) delegate).close();
  }
}
