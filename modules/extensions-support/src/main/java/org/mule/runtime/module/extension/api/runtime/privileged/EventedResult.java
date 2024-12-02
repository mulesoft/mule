/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.runtime.privileged;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.Result;

/**
 * Specialization of {@link org.mule.runtime.core.privileged.event.EventedResult} kept merely for backwards compatibility with
 * privileged extensions.
 *
 * @since 4.0
 * @deprecated since 4.8. Mule code should use {@link org.mule.runtime.core.privileged.event.EventedResult} instead. Privileged
 *             artifacts should stop using it.
 */
@Deprecated
public final class EventedResult<T, A> extends org.mule.runtime.core.privileged.event.EventedResult<T, A> {

  private EventedResult(CoreEvent event) {
    super(event);
  }

  public static <T, A> EventedResult<T, A> from(CoreEvent event) {
    return new EventedResult<>(event);
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
        return from(product.message(message.build()).build());
      }
    };
  }
}
