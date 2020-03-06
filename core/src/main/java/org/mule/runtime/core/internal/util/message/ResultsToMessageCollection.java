/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Wraps a {@link Collection} of {@link Result} instances and exposes
 * its contents as {@link Message} instances.
 *
 * This allows to avoid preemptive transformations of an entire collection
 * of {@link Result} to {@link Message}
 *
 * @since 4.0
 */
abstract class ResultsToMessageCollection implements Collection<Message> {

  private final Collection<Object> delegate;
  protected final CursorProviderFactory cursorProviderFactory;
  protected final BaseEventContext eventContext;
  protected final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  protected final Lock readLock = readWriteLock.readLock();
  protected final Lock writeLock = readWriteLock.writeLock();

  public ResultsToMessageCollection(Collection<Object> delegate,
                                    CursorProviderFactory cursorProviderFactory,
                                    BaseEventContext eventContext) {
    this.delegate = delegate;
    this.cursorProviderFactory = cursorProviderFactory;
    this.eventContext = eventContext;
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
    return new ResultToMessageIterator(delegate.iterator(), cursorProviderFactory, eventContext);
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
        .map(result -> toMessage(result, cursorProviderFactory, eventContext))
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
            return toMessage(o, cursorProviderFactory, eventContext);
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
          itemRemoved = delegate.remove(toMessage(value, cursorProviderFactory, eventContext));
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
    return delegate.removeIf(result -> filter.test(toMessage(result, cursorProviderFactory, eventContext)));
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
    return delegate.stream().map(result -> toMessage(result, cursorProviderFactory, eventContext)).collect(toList())
        .spliterator();
  }

  @Override
  public Stream<Message> stream() {
    return delegate.stream().map(result -> toMessage(result, cursorProviderFactory, eventContext));
  }

  @Override
  public Stream<Message> parallelStream() {
    return delegate.parallelStream().map(result -> toMessage(result, cursorProviderFactory, eventContext));
  }

  @Override
  public void forEach(Consumer<? super Message> action) {
    stream().forEach(action);
  }

  protected Message toMessage(Object value, CursorProviderFactory cursorProviderFactory, BaseEventContext eventContext) {
    if (value instanceof Message) {
      return (Message) value;
    } else {
      return MessageUtils.toMessage((Result) value, cursorProviderFactory, eventContext);
    }
  }
}
