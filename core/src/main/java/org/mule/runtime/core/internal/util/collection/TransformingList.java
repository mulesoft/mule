/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.collection;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;

/**
 * Specialization of {@link TransformingCollection} for collections that implement the {@link List} interface.
 *
 * @param <T> the generic type of the transformed collection's item
 * @since 4.4.0
 */
public class TransformingList<T> extends TransformingCollection<T> implements List<T> {

  private final List<Object> delegate;

  /**
   * Creates a new instance in which the given {@code transform} will be applied to all items
   *
   * @param delegate    the decorated list
   * @param transformer the transformer
   */
  public TransformingList(List<Object> delegate, Function<Object, T> transformer) {
    super(delegate, transformer);
    this.delegate = delegate;
  }

  /**
   * Creates a new instance in which the given {@code transform} will <b>ONLY</b> be applied to items which are not instances of
   * the {@code targetType}
   *
   * @param delegate    the decorated list
   * @param transformer the transformer
   * @param targetType  the expected type of the transformed instances.
   */
  public TransformingList(List<Object> delegate, Function<Object, T> transformer, Class<T> targetType) {
    super(delegate, transformer, targetType);
    this.delegate = delegate;
  }

  @Override
  public void add(int index, T element) {
    writeLock.lock();
    try {
      delegate.add(index, element);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public boolean addAll(int index, Collection<? extends T> c) {
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

      if (i == -1 && isTargetInstance(o)) {
        readLock.unlock();
        writeLock.lock();
        try {
          i = delegate.indexOf(o);
          if (i == -1) {
            transformAll();
          }
        } finally {
          readLock.lock();
          writeLock.unlock();
        }
        i = delegate.indexOf(o);
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
      if (i == -1 && isTargetInstance(o)) {
        readLock.unlock();
        writeLock.lock();
        try {
          i = delegate.lastIndexOf(o);
          if (i == -1) {
            transformAll();
          }
        } finally {
          readLock.lock();
          writeLock.unlock();
        }
        i = delegate.lastIndexOf(o);
      }

      return i;
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void sort(Comparator<? super T> c) {
    writeLock.lock();
    try {
      delegate.sort((o1, o2) -> c.compare(transformer.apply(o1), transformer.apply(o2)));
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public T get(int index) {
    readLock.lock();
    try {
      Object value = delegate.get(index);
      if (isTargetInstance(value)) {
        return (T) value;
      }
      readLock.unlock();
      writeLock.lock();
      try {
        Object update = delegate.get(index);
        if (isTargetInstance(update)) {
          return (T) update;
        }
        update = transformer.apply(update);
        delegate.set(index, update);

        return (T) update;
      } finally {
        readLock.lock();
        writeLock.unlock();
      }
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public T set(int index, T item) {
    writeLock.lock();
    try {
      Object previous = delegate.set(index, item);
      return previous != null ? transformer.apply(previous) : null;
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public T remove(int index) {
    writeLock.lock();
    try {
      Object previous = delegate.remove(index);
      return previous != null ? transformer.apply(previous) : null;
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public Iterator<T> iterator() {
    return listIterator();
  }

  @Override
  public ListIterator<T> listIterator() {
    return new ListIteratorAdapter(this);
  }

  @Override
  public ListIterator<T> listIterator(int index) {
    return new ListIteratorAdapter(this);
  }

  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    readLock.lock();
    try {
      List results = delegate.subList(fromIndex, toIndex);
      return new TransformingList(results, transformer, targetType);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  protected void transformAll() {
    for (int i = 0; i < delegate.size(); i++) {
      Object value = delegate.get(i);
      delegate.set(i, transformer.apply(value));
    }
    transformAllInvoked = true;
  }
}
