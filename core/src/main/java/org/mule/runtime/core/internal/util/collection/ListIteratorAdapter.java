/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.collection;

import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Adapts a {@link List} into a {@link ListIterator}
 *
 * @since 4.4.0
 */
public final class ListIteratorAdapter<T> implements ListIterator<T> {

  private final List<T> delegate;
  private final int size;
  private int index;
  private int lastIndex = 0;

  public ListIteratorAdapter(List<T> delegate) {
    this(delegate, 0);
  }

  ListIteratorAdapter(List<T> delegate, int startIndex) {
    this.delegate = delegate;
    index = startIndex;
    size = delegate.size();
  }

  @Override
  public boolean hasNext() {
    return index < size;
  }

  @Override
  public T next() {
    lastIndex = index++;
    return delegate.get(lastIndex);
  }

  @Override
  public boolean hasPrevious() {
    return index > 0;
  }

  @Override
  public T previous() {
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
  public void remove() {
    delegate.remove(lastIndex);
  }

  @Override
  public void set(T item) {
    delegate.set(lastIndex, item);
  }

  @Override
  public void add(T item) {
    delegate.add(index, item);
  }
}
