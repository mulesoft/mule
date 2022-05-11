/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.collection;

import static java.util.stream.Collectors.toList;

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
 * Decorates a {@link Collection} with items of random types and uses a {@link Function} to guarantee that, when exposed, those
 * items have been transformed.
 * <p>
 * This allows to lazily transform the items of the collection without making a full copy of the collection
 *
 * @since 4.4.0
 */
public class TransformingCollection<T> implements Collection<T> {

  private Collection<Object> delegate;
  protected final Class<T> targetType;
  protected final Function<Object, T> transformer;
  protected final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  protected final Lock readLock = readWriteLock.readLock();
  protected final Lock writeLock = readWriteLock.writeLock();
  protected boolean transformAllInvoked = false;

  /**
   * Creates a new instance in which the given {@code transform} will be applied to all items
   *
   * @param delegate    the decorated collection
   * @param transformer the transformer
   */
  public TransformingCollection(Collection<Object> delegate, Function<Object, T> transformer) {
    this(delegate, transformer, null);
  }

  /**
   * Creates a new instance in which the given {@code transform} will <b>ONLY</b> be applied to items which are not instances of
   * the {@code targetType}
   *
   * @param delegate    the decorated collection
   * @param transformer the transformer
   * @param targetType  the expected type of the transformed instances.
   */
  public TransformingCollection(Collection<Object> delegate, Function<Object, T> transformer, Class<T> targetType) {
    this.delegate = delegate;
    this.targetType = targetType;
    if (targetType != null) {
      this.transformer = value -> isTargetInstance(value)
          ? (T) value
          : transformer.apply(value);
    } else {
      this.transformer = transformer;
    }
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
      if (transformAllInvoked) {
        return contains;
      } else if (!contains && isTargetInstance(o)) {
        readLock.unlock();
        writeLock.lock();
        try {
          contains = delegate.contains(o);
          if (!contains) {
            transformAll();
          }
        } finally {
          readLock.lock();
          writeLock.unlock();
        }
      }
      return delegate.contains(o);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public Iterator<T> iterator() {
    return transformAllInvoked ? (Iterator<T>) delegate.iterator() : TransformingIterator.from(delegate.iterator(), transformer);
  }

  @Override
  public Object[] toArray() {
    readLock.lock();
    try {
      return transformAllInvoked ? delegate.toArray() : transformArray(delegate.toArray());
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public <T> T[] toArray(T[] a) {
    readLock.lock();
    try {
      return transformAllInvoked ? delegate.toArray(a) : transformArray(delegate.toArray(a));
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
  public boolean add(T o) {
    writeLock.lock();
    try {
      return delegate.add(o);
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

  protected <T> Collection<T> transformedCopy(Collection<?> items) {
    return (Collection<T>) items.stream().map(transformer::apply).collect(toList());
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
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
  public boolean removeIf(Predicate<? super T> filter) {
    return delegate.removeIf(result -> filter.test(transformer.apply(result)));
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return delegate.retainAll(transformedCopy(c));
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
  public Spliterator<T> spliterator() {
    if (transformAllInvoked) {
      return (Spliterator<T>) delegate.spliterator();
    } else {
      return stream().collect(toList()).spliterator();
    }
  }

  @Override
  public Stream<T> stream() {
    if (transformAllInvoked) {
      return (Stream<T>) delegate.stream();
    } else {
      return delegate.stream().map(transformer::apply);
    }
  }

  @Override
  public Stream<T> parallelStream() {
    if (transformAllInvoked) {
      return (Stream<T>) delegate.parallelStream();
    } else {
      return delegate.parallelStream().map(transformer::apply);
    }
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    stream().forEach(action);
  }

  protected boolean isTargetInstance(Object o) {
    return targetType != null ? targetType.isInstance(o) : false;
  }

  protected void transformAll() {
    delegate = transformedCopy(delegate);
    transformAllInvoked = true;
  }
}
