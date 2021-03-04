/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.collection;

import org.mule.runtime.api.streaming.HasSize;
import org.mule.runtime.core.api.streaming.iterator.StreamingIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Decorates an {@link Iterator} with items of random types and uses a {@link Function} to guarantee that, when exposed, those
 * items have been transformed.
 * <p>
 * This allows to lazily transform the iterated items without the need to fully consuming the stream and generate a new iterator
 * <p>
 * This class is also aware of the {@link StreamingIterator} and {@link HasSize} contracts. If the decorated iterator implements
 * any of those interface, so will the instance returned by {@link #from(Iterator, Function)}
 *
 * @since 4.4.0
 */
public class TransformingIterator<T> implements Iterator<T> {

  protected final Iterator<?> delegate;
  private final Function<Object, T> transformer;

  /**
   * Creates a new instance
   *
   * @param delegate    the decorated iterator
   * @param transformer the transformer function
   * @param <T>         the generic type of the transformed items
   * @return a new {@link TransformingIterator}
   */
  public static <T> TransformingIterator<T> from(Iterator<?> delegate, Function<Object, T> transformer) {
    if (delegate instanceof StreamingIterator) {
      return new TransformingStreamingIterator<>((StreamingIterator) delegate, transformer);
    } else if (delegate instanceof HasSize) {
      return new TransformingSizedIterator<>(delegate, transformer);
    } else {
      return new TransformingIterator<>(delegate, transformer);
    }
  }

  private TransformingIterator(Iterator<?> delegate, Function<Object, T> transformer) {
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
  public void forEachRemaining(Consumer<? super T> action) {
    delegate.forEachRemaining(value -> action.accept(transformer.apply(value)));
  }

  private static class TransformingSizedIterator<T> extends TransformingIterator<T> implements HasSize {

    private TransformingSizedIterator(Iterator<?> delegate, Function<Object, T> transformer) {
      super(delegate, transformer);
    }

    @Override
    public int getSize() {
      return ((HasSize) delegate).getSize();
    }
  }

  private static class TransformingStreamingIterator<T> extends TransformingIterator<T> implements StreamingIterator<T> {

    private TransformingStreamingIterator(StreamingIterator<?> delegate, Function<Object, T> transformer) {
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
}
