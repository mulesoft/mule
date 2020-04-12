/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.mediatype;

import org.mule.runtime.api.streaming.HasSize;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * {@code ListIterator<Result>} that decorates each of its delegate elements using a {@link PayloadMediaTypeResolver}
 *
 * This allows to avoid preemptive decoration of an entire collection of {@link Result}
 *
 * @since 4.2
 */
public class MediaTypeDecoratedResultListIterator implements ListIterator<Result>, HasSize {

  private final MediaTypeDecoratedResultList delegate;
  private final int size;
  private int index;
  private int lastIndex = 0;

  public MediaTypeDecoratedResultListIterator(MediaTypeDecoratedResultList delegate) {
    this(delegate, 0);
  }

  public MediaTypeDecoratedResultListIterator(MediaTypeDecoratedResultList delegate, int startIndex) {
    this.delegate = delegate;
    index = startIndex;
    size = delegate.size();
  }

  @Override
  public boolean hasNext() {
    return index < size;
  }

  @Override
  public Result next() {
    lastIndex = index++;
    return delegate.get(lastIndex);
  }

  @Override
  public boolean hasPrevious() {
    return index > 0;
  }

  @Override
  public Result previous() {
    if (index == 0) {
      throw new NoSuchElementException();
    }

    lastIndex = this.index - 1;
    return delegate.get(lastIndex);
  }

  @Override
  public int nextIndex() {
    return index < size ? index : size;
  }

  @Override
  public int previousIndex() {
    return index > 0 ? index - 1 : -1;
  }

  @Override
  public int getSize() {
    return delegate instanceof HasSize ? ((HasSize) delegate).getSize() : -1;
  }

  @Override
  public void remove() {
    delegate.remove(lastIndex);
  }

  @Override
  public void set(Result result) {
    delegate.set(lastIndex, result);
  }

  @Override
  public void add(Result result) {
    delegate.add(index, result);
  }
}

