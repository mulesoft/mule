/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Optional;

public class EventBasedResult<T, A> extends Result<T, A> {

  private final InternalEvent event;

  private EventBasedResult(InternalEvent event) {
    this.event = event;
  }

  public static <T, A> EventBasedResult<T, A> from(InternalEvent event) {
    return new EventBasedResult<>(event);
  }

  public InternalEvent getEvent() {
    return event;
  }

  @Override
  public Builder<T, A> copy() {
    final InternalEvent.Builder product = InternalEvent.builder(event);
    final Message.Builder message = Message.builder(event.getMessage());

    return new Result.Builder<T, A>() {

      @Override
      public Builder<T, A> output(T output) {
        message.payload(TypedValue.of(output));
        return this;
      }

      @Override
      public Builder<T, A> attributes(A attributes) {
        message.attributes(TypedValue.of(attributes));
        return this;
      }

      @Override
      public Builder<T, A> mediaType(MediaType mediaType) {
        message.mediaType(mediaType);
        return this;
      }

      @Override
      public Builder<T, A> attributesMediaType(MediaType mediaType) {
        message.attributesMediaType(mediaType);
        return this;
      }

      @Override
      public Result<T, A> build() {
        return EventBasedResult.from(product.message(message.build()).build());
      }
    };
  }

  @Override
  public T getOutput() {
    return (T) event.getMessage().getPayload().getValue();
  }

  @Override
  public Optional<A> getAttributes() {
    return Optional.ofNullable((A) event.getMessage().getAttributes().getValue());
  }

  @Override
  public Optional<MediaType> getMediaType() {
    return Optional.ofNullable(event.getMessage().getPayload().getDataType().getMediaType());
  }

  @Override
  public Optional<MediaType> getAttributesMediaType() {
    return Optional.ofNullable(event.getMessage().getAttributes().getDataType().getMediaType());
  }

  @Override
  public Optional<Long> getLength() {
    return event.getMessage().getPayload().getLength();
  }
}
