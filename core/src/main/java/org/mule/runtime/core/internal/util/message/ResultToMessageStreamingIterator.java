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
import org.mule.runtime.core.api.streaming.iterator.StreamingIterator;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Wraps an {@link StreamingIterator} of {@link Result} instances and exposes its contents as {@link Message} instances.
 *
 * @since 4.0
 */
final class ResultToMessageStreamingIterator implements StreamingIterator<Message> {

  private final StreamingIterator<Result> delegate;
  private final CursorProviderFactory cursorProviderFactory;
  private final BaseEventContext eventContext;
  private final ComponentLocation originatingLocation;

  ResultToMessageStreamingIterator(StreamingIterator<Result> delegate,
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
    return toMessage(delegate.next(), cursorProviderFactory, eventContext, originatingLocation);
  }

  @Override
  public void remove() {
    delegate.remove();
  }

  @Override
  public void forEachRemaining(Consumer<? super Message> action) {
    delegate.forEachRemaining(result -> action
        .accept(toMessage(result, cursorProviderFactory, eventContext, originatingLocation)));
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }
}
