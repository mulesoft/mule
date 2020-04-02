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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * {@code Collection<Result>} that decorates each of its delegate elements using a {@link PayloadMediaTypeResolver}
 *
 * This allows to avoid preemptive decoration of an entire collection of {@link Result}
 *
 * @since 4.2
 */
public class MediaTypeDecoratedResultCollection implements Collection<Result> {

  private Collection<Result> delegate;
  protected PayloadMediaTypeResolver payloadMediaTypeResolver;

  public MediaTypeDecoratedResultCollection(Collection<Result> delegate, PayloadMediaTypeResolver payloadMediaTypeResolver) {
    this.delegate = delegate;
    this.payloadMediaTypeResolver = payloadMediaTypeResolver;
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
    if (o instanceof Result) {
      return delegate.contains(payloadMediaTypeResolver.resolve((Result) o));
    }
    return delegate.contains(o);
  }

  @Override
  public Iterator<Result> iterator() {
    return new MediaTypeDecoratedResultIterator(delegate.iterator(), payloadMediaTypeResolver);
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
        .map(o -> o instanceof Result ? payloadMediaTypeResolver.resolve((Result) o) : o)
        .toArray(Object[]::new);
  }

  @Override
  public boolean add(Result result) {
    return delegate.add(result);
  }

  @Override
  public boolean remove(Object o) {
    if (o instanceof Result) {
      return delegate.remove(payloadMediaTypeResolver.resolve((Result) o));
    }
    return delegate.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return c.stream().allMatch(delegate::contains);
  }

  @Override
  public boolean addAll(Collection<? extends Result> c) {
    return delegate.addAll(c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return c.stream().map(delegate::remove).anyMatch(deleted -> deleted);
  }

  @Override
  public boolean removeIf(Predicate<? super Result> filter) {
    return delegate.removeIf(result -> filter.test(payloadMediaTypeResolver.resolve(result)));
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return delegate
        .retainAll(c.stream().map(item -> item instanceof Result ? payloadMediaTypeResolver.resolve((Result) item) : item)
            .collect(Collectors.toSet()));
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public Spliterator<Result> spliterator() {
    return delegate.stream().map(payloadMediaTypeResolver::resolve).collect(toList())
        .spliterator();
  }

  @Override
  public Stream<Result> stream() {
    return delegate.stream().map(payloadMediaTypeResolver::resolve);
  }

  @Override
  public Stream<Result> parallelStream() {
    return delegate.parallelStream().map(payloadMediaTypeResolver::resolve);
  }

}
