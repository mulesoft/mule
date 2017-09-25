/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.internal.util.concurrent.FunctionalReadWriteLock.readWriteLock;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.util.concurrent.FunctionalReadWriteLock;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
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
  protected final CoreEvent event;
  protected final FunctionalReadWriteLock lock = readWriteLock();

  public ResultsToMessageCollection(Collection<Object> delegate,
                                    CursorProviderFactory cursorProviderFactory,
                                    CoreEvent event) {
    this.delegate = delegate;
    this.cursorProviderFactory = cursorProviderFactory;
    this.event = event;
  }

  @Override
  public int size() {
    return lock.withReadLock(r -> delegate.size());
  }

  @Override
  public boolean isEmpty() {
    return lock.withReadLock(r -> delegate.isEmpty());
  }

  @Override
  public boolean contains(Object o) {
    return lock.withReadLock(r -> {
      boolean contains = delegate.contains(o);
      if (!contains && o instanceof Message) {
        contains = delegate.contains(Result.builder((Message) o));
      }

      return contains;
    });
  }

  @Override
  public Iterator<Message> iterator() {
    return new ResultToMessageIterator(delegate.iterator(), cursorProviderFactory, event);
  }

  @Override
  public Object[] toArray() {
    return lock.withReadLock(r -> transformArray(delegate.toArray()));
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return lock.withReadLock(r -> transformArray(delegate.toArray(a)));
  }

  private <T> T[] transformArray(T[] array) {
    return (T[]) Stream.of(array)
        .map(result -> toMessage(result, cursorProviderFactory, event))
        .toArray(Object[]::new);
  }

  @Override
  public boolean add(Message message) {
    return lock.withWriteLock(() -> delegate.add(message));
  }

  @Override
  public boolean remove(Object o) {
    return lock.withWriteLock(() -> delegate.remove(o));
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    if (c == null) {
      throw new NullPointerException();
    }
    return lock.withReadLock(r -> delegate.stream().allMatch(delegate::contains));
  }

  protected Collection<?> toResults(Collection<?> messages) {
    return messages.stream()
        .map(o -> {
          if (o instanceof Message) {
            return o;
          } else {
            return toMessage(o, cursorProviderFactory, event);
          }
        }).collect(toList());
  }

  @Override
  public boolean addAll(Collection<? extends Message> c) {
    return lock.withWriteLock(() -> delegate.addAll(c));
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    if (c == null) {
      throw new NullPointerException();
    }
    return lock.withWriteLock(() -> {
      boolean removed = false;
      for (Object value : c) {
        boolean itemRemoved = delegate.remove(c);
        if (!itemRemoved) {
          itemRemoved = delegate.remove(toMessage(value, cursorProviderFactory, event));
        }

        removed = removed || itemRemoved;
      }

      return removed;
    });
  }

  @Override
  public boolean removeIf(Predicate<? super Message> filter) {
    return delegate.removeIf(result -> filter.test(toMessage(result, cursorProviderFactory, event)));
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return delegate.retainAll(toResults((Collection<Message>) c));
  }

  @Override
  public void clear() {
    lock.withWriteLock(delegate::clear);
  }

  @Override
  public boolean equals(Object o) {
    return lock.withReadLock(r -> delegate.equals(o));
  }

  @Override
  public int hashCode() {
    return lock.withReadLock(r -> delegate.hashCode());
  }

  @Override
  public Spliterator<Message> spliterator() {
    return delegate.stream().map(result -> toMessage(result, cursorProviderFactory, event)).collect(toList())
        .spliterator();
  }

  @Override
  public Stream<Message> stream() {
    return delegate.stream().map(result -> toMessage(result, cursorProviderFactory, event));
  }

  @Override
  public Stream<Message> parallelStream() {
    return delegate.parallelStream().map(result -> toMessage(result, cursorProviderFactory, event));
  }

  @Override
  public void forEach(Consumer<? super Message> action) {
    stream().forEach(action);
  }

  protected Message toMessage(Object value, CursorProviderFactory cursorProviderFactory, CoreEvent event) {
    if (value instanceof Message) {
      return (Message) value;
    } else {
      return MessageUtils.toMessage((Result) value, cursorProviderFactory, event);
    }
  }
}
