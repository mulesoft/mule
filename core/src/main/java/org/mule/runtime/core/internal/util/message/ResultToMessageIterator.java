/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessage;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
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
final class ResultToMessageIterator implements Iterator<Message> {

  private final Iterator<Object> delegate;
  private final CursorProviderFactory cursorProviderFactory;
  private final CoreEvent event;

  ResultToMessageIterator(Iterator<Object> delegate,
                          CursorProviderFactory cursorProviderFactory,
                          CoreEvent event) {
    this.delegate = delegate;
    this.cursorProviderFactory = cursorProviderFactory;
    this.event = event;
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

    return toMessage((Result) delegate.next(), cursorProviderFactory, event);
  }

  @Override
  public void remove() {
    delegate.remove();
  }

  @Override
  public void forEachRemaining(Consumer<? super Message> action) {
    delegate.forEachRemaining(value -> {
      if (value instanceof Result) {
        value = toMessage((Result) value, cursorProviderFactory, event);
      }
      action.accept((Message) value);
    });
  }
}
