/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.HasSize;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Decorates an {@link Iterator} of elements of random types using a {@link Function} which guarantees that the
 * items are always surfaced as a {@link Message}
 *
 * This allows to avoid preemptive transformations of an entire dataset
 *
 * @since 4.4.0
 */
public class TransformingIterator<T> implements Iterator<T>, HasSize {

  protected final Iterator<?> delegate;
  private final Function<Object, T> transformer;

  public TransformingIterator(Iterator<?> delegate, Function<Object, T> transformer) {
    this.delegate = delegate;
    this.transformer = transformer;
  }

  @Override
  public boolean hasNext() {
    return delegate.hasNext();
  }

  @Override
  public T next() {
    Object value = delegate.next();
    return transformer.apply(value);
  }

  @Override
  public void remove() {
    delegate.remove();
  }

  @Override
  public int getSize() {
    return delegate instanceof HasSize ? ((HasSize) delegate).getSize() : -1;
  }

  @Override
  public void forEachRemaining(Consumer<? super T> action) {
    delegate.forEachRemaining(value -> action.accept(transformer.apply(value)));
  }
}
