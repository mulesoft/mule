/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessage;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.HasSize;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Wraps an {@link Iterator} of {@link Result} instances and exposes
 * its contents as {@link Message} instances.
 *
 * This allows to avoid preemptive transformations of an entire collection
 * of {@link Result} to {@link Message}
 *
 * @since 4.0
 */
final class ResultToMessageIterator implements Iterator<Message>, HasSize {

  private final Iterator<Object> delegate;
  private final CursorProviderFactory cursorProviderFactory;
  private final BaseEventContext eventContext;
  private final ComponentLocation originatingLocation;

  ResultToMessageIterator(Iterator<Object> delegate,
                          CursorProviderFactory cursorProviderFactory,
                          BaseEventContext eventContext,
                          ComponentLocation originatingLocation) {
    this.delegate = delegate;
    this.cursorProviderFactory = cursorProviderFactory;
    this.eventContext = eventContext;
    this.originatingLocation = originatingLocation;
  }

  @Override
  public boolean hasNext() {
    return delegate.hasNext();
  }

  @Override
  public Message next() {
    Object value = delegate.next();
    if (value instanceof Message) {
      return (Message) value;
    }

    return toMessage((Result) value, cursorProviderFactory, eventContext, originatingLocation);
  }

  @Override
  public void remove() {
    delegate.remove();
  }

  @Override
  public int getSize() {
    return delegate instanceof HasSize ? ((HasSize) delegate).getSize() : -1;
  }

  @Override
  public void forEachRemaining(Consumer<? super Message> action) {
    delegate.forEachRemaining(value -> {
      if (value instanceof Result) {
        value = toMessage((Result) value, cursorProviderFactory, eventContext, originatingLocation);
      }
      action.accept((Message) value);
    });
  }
}
