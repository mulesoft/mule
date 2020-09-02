/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;

/**
 * Specialization of {@link TransformedMessageCollection} for collections that implement the {@link List}
 * interface
 *
 * @since 4.4.0
 */
public final class TransformedMessageList extends TransformedMessageCollection implements List<Message> {

  private final List<Object> delegate;

  public TransformedMessageList(List<Object> delegate, Function<Object, Message> transformer) {
    super(delegate, transformer);
    this.delegate = delegate;
  }

  @Override
  public void add(int index, Message element) {
    writeLock.lock();
    try {
      delegate.add(index, element);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public boolean addAll(int index, Collection<? extends Message> c) {
    writeLock.lock();
    try {
      return delegate.addAll(index, c);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public int indexOf(Object o) {
    readLock.lock();
    try {
      int i = delegate.indexOf(o);
      if (i == -1 && o instanceof Message) {
        i = delegate.indexOf(Result.builder((Message) o).build());
      }

      return i;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public int lastIndexOf(Object o) {
    readLock.lock();
    try {
      int i = delegate.lastIndexOf(o);
      if (i == -1 && o instanceof Message) {
        i = delegate.lastIndexOf(Result.builder((Message) o).build());
      }

      return i;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void sort(Comparator<? super Message> c) {
    writeLock.lock();
    try {
      delegate.sort((o1, o2) -> c.compare(transformer.apply(o1), transformer.apply(o2)));
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public Message get(int index) {
    readLock.lock();
    try {
      Object value = delegate.get(index);
      if (value instanceof Message) {
        return (Message) value;
      }
      readLock.unlock();
      writeLock.lock();
      try {
        Object update = delegate.get(index);
        if (update instanceof Message) {
          return (Message) update;
        }
        update = transformer.apply(update);
        delegate.set(index, update);

        return (Message) update;
      } finally {
        readLock.lock();
        writeLock.unlock();
      }
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public Message set(int index, Message message) {
    writeLock.lock();
    try {
      Object previous = delegate.set(index, message);
      return previous != null ? transformer.apply(previous) : null;
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public Message remove(int index) {
    writeLock.lock();
    try {
      Object previous = delegate.remove(index);
      return previous != null ? transformer.apply(previous) : null;
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public Iterator<Message> iterator() {
    return listIterator();
  }

  @Override
  public ListIterator<Message> listIterator() {
    return new TransformedMessageListIterator(this);
  }

  @Override
  public ListIterator<Message> listIterator(int index) {
    return new TransformedMessageListIterator(this);
  }

  @Override
  public List<Message> subList(int fromIndex, int toIndex) {
    readLock.lock();
    try {
      List results = delegate.subList(fromIndex, toIndex);
      return new TransformedMessageList(results, transformer);
    } finally {
      readLock.unlock();
    }
  }
}
