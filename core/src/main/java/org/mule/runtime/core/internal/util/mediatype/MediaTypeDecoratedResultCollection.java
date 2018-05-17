/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.mediatype;

import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @since 4.2
 */
public class MediaTypeDecoratedResultCollection implements Collection<Result> {

  private Collection<Result> delegate;
  protected MediaTypeResolver mediaTypeResolver;

  public MediaTypeDecoratedResultCollection(Collection<Result> delegate, MediaTypeResolver mediaTypeResolver) {
    this.delegate = delegate;
    this.mediaTypeResolver = mediaTypeResolver;
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    boolean contains = delegate.contains(o);
    if (!contains && o instanceof Result) {
      contains = delegate.contains(mediaTypeResolver.resolve((Result) o));
    }
    return contains;
  }

  @Override
  public Iterator<Result> iterator() {
    return new MediaTypeDecoratedResultIterator(delegate.iterator(), mediaTypeResolver);
  }

  @Override
  public Object[] toArray() {
    return transformArray(delegate.toArray());
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return transformArray(delegate.toArray(a));
  }

  private <T> T[] transformArray(T[] array) {
    return (T[]) Stream.of(array)
        .map(o -> o instanceof Result ? mediaTypeResolver.resolve((Result) o) : o)
        .toArray(Object[]::new);
  }

  @Override
  public boolean add(Result result) {
    return delegate.add(result);
  }

  @Override
  public boolean remove(Object o) {
    boolean itemRemoved = delegate.remove(o);
    if (!itemRemoved && o instanceof Result) {
      itemRemoved = delegate.remove(mediaTypeResolver.resolve((Result) o));
    }
    return itemRemoved;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    if (c == null) {
      throw new NullPointerException();
    }
    return c.stream().allMatch(this::contains);
  }

  @Override
  public boolean addAll(Collection<? extends Result> c) {
    return delegate.addAll(c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return c.stream().map(this::remove).anyMatch(p -> p);
  }

  @Override
  public boolean removeIf(Predicate<? super Result> filter) {
    return delegate.removeIf(result -> filter.test(mediaTypeResolver.resolve(result)));
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return delegate.retainAll(c);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public Spliterator<Result> spliterator() {
    return delegate.stream().map(mediaTypeResolver::resolve).collect(toList())
        .spliterator();
  }

  @Override
  public Stream<Result> stream() {
    return delegate.stream().map(mediaTypeResolver::resolve);
  }

  @Override
  public Stream<Result> parallelStream() {
    return delegate.parallelStream().map(mediaTypeResolver::resolve);
  }

}
