/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessage;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
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
public class ResultsToMessageCollection implements Collection<Message> {

  private final Collection<Result> delegate;
  protected final CursorProviderFactory cursorProviderFactory;
  protected final Event event;

  public ResultsToMessageCollection(Collection<Result> delegate,
                                    CursorProviderFactory cursorProviderFactory,
                                    Event event) {
    this.delegate = delegate;
    this.cursorProviderFactory = cursorProviderFactory;
    this.event = event;
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
    return delegate.contains(o);
  }

  @Override
  public Iterator<Message> iterator() {
    Iterator<Result> iterator = delegate.iterator();
    return new ResultToMessageIterator(iterator, cursorProviderFactory, event);
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
        .map(result -> toMessage((Result) result, cursorProviderFactory, event))
        .toArray(Object[]::new);
  }

  @Override
  public boolean add(Message message) {
    return delegate.add(Result.builder(message).build());
  }

  @Override
  public boolean remove(Object o) {
    return delegate.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return delegate.containsAll(toResults((Collection<Message>) c));
  }

  protected Collection<Result> toResults(Collection<? extends Message> messages) {
    return messages.stream()
        .map(message -> Result.builder(message).build())
        .collect(toList());
  }

  @Override
  public boolean addAll(Collection<? extends Message> c) {
    return delegate.addAll(toResults(c));
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return delegate.removeAll(toResults((Collection<Message>) c));
  }

  @Override
  public boolean removeIf(Predicate<? super Message> filter) {
    return delegate.removeIf(result -> filter.test(toMessage(result)));
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return delegate.retainAll(toResults((Collection<Message>) c));
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public boolean equals(Object o) {
    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
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
}
