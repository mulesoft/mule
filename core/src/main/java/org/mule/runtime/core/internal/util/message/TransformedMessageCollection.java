/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Decorates a {@link Collection} with items of random types and uses a {@link Function} 
 * to guarantee that those items always surfaced in the form of a {@link Message}
 *
 * This allows to avoid preemptive transformations of an entire collection.
 *
 * @since 4.4.0
 */
abstract class TransformedMessageCollection implements Collection<Message> {

  private final Collection<Object> delegate;
  protected final Function<Object, Message> transformer;
  protected final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  protected final Lock readLock = readWriteLock.readLock();
  protected final Lock writeLock = readWriteLock.writeLock();

  public TransformedMessageCollection(Collection<Object> delegate, Function<Object, Message> transformer) {
    this.delegate = delegate;
    this.transformer = value -> {
      if (value instanceof Message) {
        return (Message) value;
      } else {
        return transformer.apply(value);
      }
    };
  }

  @Override
  public int size() {
    readLock.lock();
    try {
      return delegate.size();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public boolean isEmpty() {
    readLock.lock();
    try {
      return delegate.isEmpty();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public boolean contains(Object o) {
    readLock.lock();
    try {
      boolean contains = delegate.contains(o);
      if (!contains && o instanceof Message) {
        contains = delegate.contains(Result.builder((Message) o));
      }
      return contains;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public Iterator<Message> iterator() {
    return new TransformedMessageIterator(delegate.iterator(), transformer);
  }

  @Override
  public Object[] toArray() {
    readLock.lock();
    try {
      return transformArray(delegate.toArray());
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public <T> T[] toArray(T[] a) {
    readLock.lock();
    try {
      return transformArray(delegate.toArray(a));
    } finally {
      readLock.unlock();
    }
  }

  private <T> T[] transformArray(T[] array) {
    return (T[]) Stream.of(array)
        .map(result -> transformer.apply(result))
        .toArray(Object[]::new);
  }

  @Override
  public boolean add(Message message) {
    writeLock.lock();
    try {
      return delegate.add(message);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public boolean remove(Object o) {
    writeLock.lock();
    try {
      return delegate.remove(o);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    if (c == null) {
      throw new NullPointerException();
    }
    readLock.lock();
    try {
      return delegate.stream().allMatch(delegate::contains);
    } finally {
      readLock.unlock();
    }
  }

  protected Collection<?> toResults(Collection<?> messages) {
    return messages.stream()
        .map(o -> {
          if (o instanceof Message) {
            return o;
          } else {
            return transformer.apply(o);
          }
        }).collect(toList());
  }

  @Override
  public boolean addAll(Collection<? extends Message> c) {
    writeLock.lock();
    try {
      return delegate.addAll(c);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    if (c == null) {
      throw new NullPointerException();
    }

    writeLock.lock();
    try {
      boolean removed = false;
      for (Object value : c) {
        boolean itemRemoved = delegate.remove(c);
        if (!itemRemoved) {
          itemRemoved = delegate.remove(transformer.apply(value));
        }

        removed = removed || itemRemoved;
      }

      return removed;
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public boolean removeIf(Predicate<? super Message> filter) {
    return delegate.removeIf(result -> filter.test(transformer.apply(result)));
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return delegate.retainAll(toResults(c));
  }

  @Override
  public void clear() {
    writeLock.lock();
    try {
      delegate.clear();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public boolean equals(Object o) {
    writeLock.lock();
    try {
      return delegate.equals(o);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public int hashCode() {
    writeLock.lock();
    try {
      return delegate.hashCode();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public Spliterator<Message> spliterator() {
    return delegate.stream().map(result -> transformer.apply(result)).collect(toList())
        .spliterator();
  }

  @Override
  public Stream<Message> stream() {
    return delegate.stream().map(result -> transformer.apply(result));
  }

  @Override
  public Stream<Message> parallelStream() {
    return delegate.parallelStream().map(result -> transformer.apply(result));
  }

  @Override
  public void forEach(Consumer<? super Message> action) {
    stream().forEach(action);
  }
}
