/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.message;

import static org.mule.runtime.core.util.message.MessageUtils.toMessage;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.internal.streaming.object.iterator.StreamingIterator;
import org.mule.runtime.core.streaming.CursorProviderFactory;
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
  private final MediaType mediaType;
  private final CursorProviderFactory cursorProviderFactory;
  private final Event event;

  ResultToMessageStreamingIterator(StreamingIterator<Result> delegate,
                                   MediaType mediaType,
                                   CursorProviderFactory cursorProviderFactory,
                                   Event event) {
    this.delegate = delegate;
    this.mediaType = mediaType;
    this.cursorProviderFactory = cursorProviderFactory;
    this.event = event;
  }

  @Override
  public boolean hasNext() {
    return delegate.hasNext();
  }

  @Override
  public Message next() {
    return toMessage(delegate.next(), mediaType, cursorProviderFactory, event);
  }

  @Override
  public void remove() {
    delegate.remove();
  }

  @Override
  public void forEachRemaining(Consumer<? super Message> action) {
    delegate.forEachRemaining(result -> action.accept(toMessage(result, mediaType, cursorProviderFactory, event)));
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
