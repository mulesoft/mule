/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.event;

import static java.util.Optional.ofNullable;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * An operation execution {@link Result} that is created based on the resulting {@link CoreEvent} of that execution. This allows
 * for executions to be concatenated by the plugin's developer without losing information of the event propagated through the
 * flow.
 *
 * NOTE: This class was originally created in 4.0 as part of the {@code org.mule.runtime.module.extension.api.runtime.privileged}
 * in the {@code mule-extensions-support} module. It was moved here in 4.8 as part of issue {@code W-16420215}, with the original
 * class left deprecated for compatibility with privileged extensions such as aggregators.
 *
 * @param <T> the generic type of the output value
 * @param <A> the generic type of the message attributes
 * @since 4.8.0
 */
@NoExtend
public class EventedResult<T, A> extends Result<T, A> {

  protected final CoreEvent event;

  protected EventedResult(CoreEvent event) {
    this.event = event;
  }

  public static <T, A> EventedResult<T, A> from(CoreEvent event) {
    return new EventedResult<>(event);
  }

  public CoreEvent getEvent() {
    return event;
  }

  @Override
  public Builder<T, A> copy() {
    final CoreEvent.Builder product = CoreEvent.builder(event);
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
        return EventedResult.from(product.message(message.build()).build());
      }
    };
  }

  @Override
  public T getOutput() {
    return (T) event.getMessage().getPayload().getValue();
  }

  @Override
  public Optional<A> getAttributes() {
    return ofNullable((A) event.getMessage().getAttributes().getValue());
  }

  @Override
  public Optional<MediaType> getMediaType() {
    return ofNullable(event.getMessage().getPayload().getDataType().getMediaType());
  }

  @Override
  public Optional<MediaType> getAttributesMediaType() {
    return ofNullable(event.getMessage().getAttributes().getDataType().getMediaType());
  }

  @Override
  public Optional<Long> getLength() {
    return event.getMessage().getPayload().getLength();
  }

  @Override
  public OptionalLong getByteLength() {
    return event.getMessage().getPayload().getByteLength();
  }
}
