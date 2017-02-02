/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.message;

import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.toMessage;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

/**
 * Wraps a {@link List} of {@link Result} instances and exposes
 * its contents as {@link Message} instances.
 *
 * This allows to avoid preemptive transformations of an entire List
 * of {@link Result} to {@link Message}
 *
 * @since 4.0
 */
public final class ResultsToMessageList extends ResultsToMessageCollection implements List<Message> {

  private final List<Result> delegate;

  public ResultsToMessageList(List<Result> delegate, MediaType mediaType) {
    super(delegate, mediaType);
    this.delegate = delegate;
  }

  @Override
  public void add(int index, Message element) {
    delegate.add(index, Result.builder(element).build());
  }

  @Override
  public boolean addAll(int index, Collection<? extends Message> c) {
    return delegate.addAll(index, toResults(c));
  }

  @Override
  public int indexOf(Object o) {
    if (o instanceof Message) {
      return delegate.indexOf(Result.builder((Message) o).build());
    }

    return -1;
  }

  @Override
  public int lastIndexOf(Object o) {
    if (o instanceof Message) {
      return delegate.lastIndexOf(Result.builder((Message) o).build());
    }

    return -1;
  }

  @Override
  public void sort(Comparator<? super Message> c) {
    delegate.sort((o1, o2) -> c.compare(toMessage(o1, mediaType), toMessage(o2, mediaType)));
  }

  @Override
  public Message get(int index) {
    return toMessage(delegate.get(index), mediaType);
  }

  @Override
  public Message set(int index, Message message) {
    Result previous = delegate.set(index, Result.builder(message).build());
    return previous != null ? toMessage(previous, mediaType) : null;
  }

  @Override
  public Message remove(int index) {
    Result previous = delegate.remove(index);
    return previous != null ? toMessage(previous, mediaType) : null;
  }

  @Override
  public ListIterator<Message> listIterator() {
    return new ResultToMessageListIterator(delegate.listIterator(), mediaType);
  }

  @Override
  public ListIterator<Message> listIterator(int index) {
    return new ResultToMessageListIterator(delegate.listIterator(index), mediaType);
  }

  @Override
  public List<Message> subList(int fromIndex, int toIndex) {
    return new ResultsToMessageList(delegate.subList(fromIndex, toIndex), mediaType);
  }

}
