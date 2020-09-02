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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Decorates an {@link StreamingIterator} of elements of random types using a {@link Function} which guarantees
 * that the items are always surfaced as a {@link Message}
 *
 * This allows to avoid preemptive transformations of an entire dataset
 *
 * @since 4.4.0
 */
final class TransformedMessageStreamingIterator implements StreamingIterator<Message> {

  private final StreamingIterator<?> delegate;
  private final Function<Object, Message> transformer;

  TransformedMessageStreamingIterator(StreamingIterator<?> delegate, Function<Object, Message> transformer) {
    this.delegate = delegate;
    this.transformer = transformer;
  }

  @Override
  public boolean hasNext() {
    return delegate.hasNext();
  }

  @Override
  public Message next() {
    return transformer.apply(delegate.next());
  }

  @Override
  public void remove() {
    delegate.remove();
  }

  @Override
  public void forEachRemaining(Consumer<? super Message> action) {
    delegate.forEachRemaining(result -> action.accept(transformer.apply(result)));
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }
}
