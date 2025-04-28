/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.message.ExceptionPayload;

import java.util.Collection;
import java.util.Iterator;

/**
 * Message
 */
@NoImplement
public interface InternalMessage extends Message {

  /**
   * Provides a builder to create {@link Message} objects.
   *
   * @return a new {@link Builder}.
   */
  static PayloadBuilder builder() {
    return DefaultMessageBuilderFactory.getInstance().create();
  }

  /**
   * Provides a builder to create {@link Message} objects based on an existing {@link Message} instance.
   *
   * @param message existing {@link Message} to use as a template to create a new {@link Builder} instance.
   * @return a new {@link Builder} based on the template {@code message} provided.
   */
  static Builder builder(Message message) {
    return DefaultMessageBuilderFactory.getInstance().create(message);
  }

  /**
   * If an error occurred during the processing of this message this will return a ErrorPayload that contains the root exception
   * and Mule error code, plus any other related info
   *
   * @return The exception payload (if any) attached to this message
   *
   * @deprecated This field is no longer populated
   */
  @Deprecated
  ExceptionPayload getExceptionPayload();

  interface PayloadBuilder extends Message.PayloadBuilder {

    @Override
    Builder payload(TypedValue<?> typedValue);

    @Override
    Builder nullValue();

    @Override
    Builder value(Object payload);

    @Override
    CollectionBuilder streamValue(Iterator payload, Class<?> itemType);

    @Override
    CollectionBuilder collectionValue(Collection payload, Class<?> itemType);

    @Override
    CollectionBuilder collectionValue(Object[] payload);
  }

  interface Builder extends Message.Builder, PayloadBuilder {

    @Override
    Builder mediaType(MediaType mediaType);

    @Override
    Builder attributes(TypedValue<?> typedValue);

    @Override
    Builder nullAttributesValue();

    @Override
    Builder attributesValue(Object value);

    @Override
    Builder attributesMediaType(MediaType mediaType);

    /**
     * @param exceptionPayload
     * @return this builder.
     *
     * @deprecated This field is no longer populated
     */
    @Deprecated
    Builder exceptionPayload(ExceptionPayload exceptionPayload);

    @Override
    InternalMessage build();
  }

  interface CollectionBuilder extends Message.CollectionBuilder, Builder {

    @Override
    CollectionBuilder itemMediaType(MediaType mediaType);

    @Override
    CollectionBuilder mediaType(MediaType mediaType);

    @Override
    CollectionBuilder attributes(TypedValue<?> typedValue);

    @Override
    CollectionBuilder nullAttributesValue();

    @Override
    CollectionBuilder attributesValue(Object o);

    @Override
    CollectionBuilder attributesMediaType(MediaType mediaType);

    /*
     * @deprecated This field is no longer populated
     */
    @Deprecated
    @Override
    CollectionBuilder exceptionPayload(ExceptionPayload exceptionPayload);

    @Override
    InternalMessage build();

  }

  interface MapBuilder extends Message.MapBuilder, Builder {

  }
}
