/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessage;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.streaming.CursorProviderFactory;
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

  private final Iterator<Result> delegate;
  private final CursorProviderFactory cursorProviderFactory;
  private final Event event;

  ResultToMessageIterator(Iterator<Result> delegate,
                          CursorProviderFactory cursorProviderFactory,
                          Event event) {
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
    return toMessage(delegate.next(), cursorProviderFactory, event);
  }

  @Override
  public void remove() {
    delegate.remove();
  }

  @Override
  public void forEachRemaining(Consumer<? super Message> action) {
    delegate.forEachRemaining(result -> action.accept(toMessage(result, cursorProviderFactory, event)));
  }
}
