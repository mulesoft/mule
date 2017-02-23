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
import org.mule.runtime.core.streaming.bytes.CursorStreamProviderFactory;
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
  private final MediaType mediaType;
  private final CursorStreamProviderFactory cursorStreamProviderFactory;
  private final Event event;

  ResultToMessageIterator(Iterator<Result> delegate,
                          MediaType mediaType,
                          CursorStreamProviderFactory cursorStreamProviderFactory,
                          Event event) {
    this.delegate = delegate;
    this.mediaType = mediaType;
    this.cursorStreamProviderFactory = cursorStreamProviderFactory;
    this.event = event;
  }

  @Override
  public boolean hasNext() {
    return delegate.hasNext();
  }


  @Override
  public Message next() {
    return toMessage(delegate.next(), mediaType, cursorStreamProviderFactory, event);
  }

  @Override
  public void remove() {
    delegate.remove();
  }

  @Override
  public void forEachRemaining(Consumer<? super Message> action) {
    delegate.forEachRemaining(result -> action.accept(toMessage(result, mediaType, cursorStreamProviderFactory, event)));
  }
}
