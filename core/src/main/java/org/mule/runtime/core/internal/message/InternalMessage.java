/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.message.ExceptionPayload;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.activation.DataHandler;

/**
 * Message
 */
public interface InternalMessage extends Message, MessageProperties, MessageAttachments {

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
   */
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
     */
    Builder exceptionPayload(ExceptionPayload exceptionPayload);

    /**
     * @param key
     * @param value
     * @return
     * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
     */
    @Deprecated
    Builder addInboundProperty(String key, Serializable value);

    /**
     * @param key
     * @param value
     * @param mediaType
     * @return
     * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
     */
    @Deprecated
    Builder addInboundProperty(String key, Serializable value, MediaType mediaType);

    /**
     * @param key
     * @param value
     * @param dataType
     * @return
     * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
     */
    @Deprecated
    Builder addInboundProperty(String key, Serializable value, DataType dataType);

    /**
     * @param key
     * @param value
     * @return
     * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
     */
    @Deprecated
    Builder addOutboundProperty(String key, Serializable value);

    /**
     * @param key
     * @param value
     * @param mediaType
     * @return
     * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
     */
    @Deprecated
    Builder addOutboundProperty(String key, Serializable value, MediaType mediaType);

    /**
     * @param key
     * @param value
     * @param dataType
     * @return
     * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
     */
    @Deprecated
    Builder addOutboundProperty(String key, Serializable value, DataType dataType);

    /**
     * @param key
     * @return
     * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
     */
    @Deprecated
    Builder removeInboundProperty(String key);

    /**
     * @param key
     * @return
     * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
     */
    @Deprecated
    Builder removeOutboundProperty(String key);

    /**
     * @param key
     * @param value
     * @return
     * @deprecated Transport infrastructure is deprecated. 
     */
    @Deprecated
    Builder addInboundAttachment(String key, DataHandler value);

    /**
     * @param key
     * @param value
     * @return
     * @deprecated Transport infrastructure is deprecated. 
     */
    @Deprecated
    Builder addOutboundAttachment(String key, DataHandler value);

    /**
     * @param key
     * @return
     * @deprecated Transport infrastructure is deprecated. 
     */
    @Deprecated
    Builder removeInboundAttachment(String key);

    /**
     * @param key
     * @return
     * @deprecated Transport infrastructure is deprecated. 
     */
    @Deprecated
    Builder removeOutboundAttachment(String key);

    /**
     * @param inboundProperties
     * @return
     * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
     */
    @Deprecated
    Builder inboundProperties(Map<String, Serializable> inboundProperties);

    /**
     * @param outboundProperties
     * @return
     * @deprecated Transport infrastructure is deprecated. Use {@link Message#getAttributes()} instead.
     */
    @Deprecated
    Builder outboundProperties(Map<String, Serializable> outboundProperties);

    /**
     * @param inboundAttachments
     * @return
     * @deprecated Transport infrastructure is deprecated. 
     */
    @Deprecated
    Builder inboundAttachments(Map<String, DataHandler> inboundAttachments);

    /**
     * @param outbundAttachments
     * @return
     * @deprecated Transport infrastructure is deprecated. 
     */
    @Deprecated
    Builder outboundAttachments(Map<String, DataHandler> outbundAttachments);

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

    @Override
    CollectionBuilder exceptionPayload(ExceptionPayload exceptionPayload);

    @Override
    CollectionBuilder addInboundProperty(String key, Serializable value);

    @Override
    CollectionBuilder addInboundProperty(String key, Serializable value, MediaType mediaType);

    @Override
    CollectionBuilder addInboundProperty(String key, Serializable value, DataType dataType);

    @Override
    CollectionBuilder addOutboundProperty(String key, Serializable value);

    @Override
    CollectionBuilder addOutboundProperty(String key, Serializable value, MediaType mediaType);

    @Override
    CollectionBuilder addOutboundProperty(String key, Serializable value, DataType dataType);

    @Override
    CollectionBuilder removeInboundProperty(String key);

    @Override
    CollectionBuilder removeOutboundProperty(String key);

    @Override
    CollectionBuilder addInboundAttachment(String key, DataHandler value);

    @Override
    CollectionBuilder addOutboundAttachment(String key, DataHandler value);

    @Override
    CollectionBuilder removeInboundAttachment(String key);

    @Override
    CollectionBuilder removeOutboundAttachment(String key);

    @Override
    CollectionBuilder inboundProperties(Map<String, Serializable> inboundProperties);

    @Override
    CollectionBuilder outboundProperties(Map<String, Serializable> outboundProperties);

    @Override
    CollectionBuilder inboundAttachments(Map<String, DataHandler> inboundAttachments);

    @Override
    CollectionBuilder outboundAttachments(Map<String, DataHandler> outbundAttachments);

    @Override
    InternalMessage build();

  }
}
