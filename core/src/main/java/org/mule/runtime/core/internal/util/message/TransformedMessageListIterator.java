/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import org.mule.runtime.api.message.Message;

import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Decorates an {@link ListIterator} of elements of random types using a {@link Function} which guarantees
 * that the items are always surfaced as a {@link Message}
 *
 * This allows to avoid preemptive transformations of an entire dataset
 *
 * @since 4.4.0
 */
final class TransformedMessageListIterator implements ListIterator<Message> {

  private final List<Message> delegate;
  private final int size;
  private int index;
  private int lastIndex = 0;

  TransformedMessageListIterator(List<Message> delegate) {
    this(delegate, 0);
  }

  TransformedMessageListIterator(List<Message> delegate, int startIndex) {
    this.delegate = delegate;
    index = startIndex;
    size = delegate.size();
  }

  @Override
  public boolean hasNext() {
    return index < size;
  }

  @Override
  public Message next() {
    lastIndex = index++;
    return delegate.get(lastIndex);
  }

  @Override
  public boolean hasPrevious() {
    return index > 0;
  }

  @Override
  public Message previous() {
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
  public void set(Message message) {
    delegate.set(lastIndex, message);
  }

  @Override
  public void add(Message message) {
    delegate.add(index, message);
  }
}
