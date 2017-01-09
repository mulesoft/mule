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

import java.util.ListIterator;
import java.util.function.Consumer;

/**
 * Wraps an {@link ListIterator} of {@link Result} instances and exposes
 * its contents as {@link Message} instances.
 *
 * This allows to avoid preemptive transformations of an entire collection
 * of {@link Result} to {@link Message}
 *
 * @since 4.0
 */
final class ResultToMessageListIterator implements ListIterator<Message> {

  private final ListIterator<Result> delegate;
  private final MediaType mediaType;

  ResultToMessageListIterator(ListIterator<Result> delegate, MediaType mediaType) {
    this.delegate = delegate;
    this.mediaType = mediaType;
  }

  @Override
  public boolean hasNext() {
    return delegate.hasNext();
  }

  @Override
  public Message next() {
    return toMessage(delegate.next(), mediaType);
  }

  @Override
  public boolean hasPrevious() {
    return delegate.hasPrevious();
  }

  @Override
  public Message previous() {
    return toMessage(delegate.previous(), mediaType);
  }

  @Override
  public int nextIndex() {
    return delegate.nextIndex();
  }

  @Override
  public int previousIndex() {
    return delegate.previousIndex();
  }

  @Override
  public void remove() {
    delegate.remove();
  }

  @Override
  public void set(Message message) {
    delegate.set(Result.builder(message).build());
  }

  @Override
  public void add(Message message) {
    delegate.add(Result.builder(message).build());
  }

  public void forEachRemaining(Consumer<? super Message> action) {
    delegate.forEachRemaining(result -> action.accept(toMessage(result, mediaType)));
  }
}
