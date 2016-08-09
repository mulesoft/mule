/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.message.Correlation;
import org.mule.runtime.core.message.DefaultMuleMessageBuilderFactory;
import org.mule.runtime.core.message.DefaultMultiPartPayload;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.activation.DataHandler;

/**
 * MuleMessage
 */
public interface MuleMessage extends org.mule.runtime.api.message.MuleMessage, MessageProperties, MessageAttachments {

  /**
   * Provides a builder to create {@link MuleMessage} objects.
   *
   * @return a new {@link Builder}.
   */
  static PayloadBuilder builder() {
    return DefaultMuleMessageBuilderFactory.getInstance().create();
  }

  /**
   * Provides a builder to create {@link MuleMessage} objects based on an existing {@link MuleMessage} instance.
   *
   * @param message existing {@link MuleMessage} to use as a template to create a new {@link Builder} instance.
   * @return a new {@link Builder} based on the template {@code message} provided.
   */
  static Builder builder(MuleMessage message) {
    return DefaultMuleMessageBuilderFactory.getInstance().create(message);
  }

  static Builder builder(org.mule.runtime.api.message.MuleMessage message) {
    return DefaultMuleMessageBuilderFactory.getInstance().create(message);
  }

  /**
   * Create a new {@link MuleMessage instance} with the given payload.
   *
   * @param payload the message payload
   * @return new message instance
   */
  static MuleMessage of(Object payload) {
    return builder().payload(payload).build();
  }

  /**
   * gets the unique identifier for the message. It's up to the implementation to ensure a unique id
   *
   * @return a unique message id. The Id should never be null. If the underlying transport does not have the notion of a message
   *         Id, one should be generated. The generated Id should be a UUID.
   */
  String getUniqueId();

  /**
   * gets an identifier that is the same among parent and child messages
   *
   * @return a message id for the group of descendant messages. The Id should never be null.
   */
  String getMessageRootId();

  /**
   * Returns the correlation metadata of this message. See {@link Correlation}.
   * 
   * @return the correlation metadata of this message.
   */
  Correlation getCorrelation();

  /**
   * If an error occurred during the processing of this message this will return a ErrorPayload that contains the root exception
   * and Mule error code, plus any other releated info
   *
   * @return The exception payload (if any) attached to this message
   */
  ExceptionPayload getExceptionPayload();

  interface PayloadBuilder extends org.mule.runtime.api.message.MuleMessage.PayloadBuilder {

    @Override
    Builder nullPayload();

    @Override
    Builder payload(Object payload);

    @Override
    CollectionBuilder streamPayload(Iterator payload, Class<?> itemType);

    @Override
    CollectionBuilder collectionPayload(Collection payload, Class<?> itemType);

    @Override
    CollectionBuilder collectionPayload(Object[] payload);
  }

  interface Builder extends org.mule.runtime.api.message.MuleMessage.Builder, PayloadBuilder {

    @Override
    Builder mediaType(MediaType mediaType);

    @Override
    Builder attributes(Attributes value);

    /**
     * @param correlationId
     * @return this builder.
     */
    Builder correlationId(String correlationId);

    /**
     * @param correlationSequence
     * @return this builder.
     */
    Builder correlationSequence(Integer correlationSequence);

    /**
     * @param correlationGroupSize
     * @return this builder.
     */
    Builder correlationGroupSize(Integer correlationGroupSize);

    /**
     * @param exceptionPayload
     * @return this builder.
     */
    Builder exceptionPayload(ExceptionPayload exceptionPayload);

    /**
     * @param id
     * @return
     */
    Builder id(String id);

    /**
     * @param rootId
     * @return
     */
    Builder rootId(String rootId);

    /**
     * @param key
     * @param value
     * @return
     */
    Builder addInboundProperty(String key, Serializable value);

    /**
     * @param key
     * @param value
     * @param mediaType
     * @return
     */
    Builder addInboundProperty(String key, Serializable value, MediaType mediaType);

    /**
     * @param key
     * @param value
     * @param dataType
     * @return
     */
    Builder addInboundProperty(String key, Serializable value, DataType dataType);

    /**
     * @param key
     * @param value
     * @return
     */
    Builder addOutboundProperty(String key, Serializable value);

    /**
     * @param key
     * @param value
     * @param mediaType
     * @return
     */
    Builder addOutboundProperty(String key, Serializable value, MediaType mediaType);

    /**
     * @param key
     * @param value
     * @param dataType
     * @return
     */
    Builder addOutboundProperty(String key, Serializable value, DataType dataType);

    /**
     * @param key
     * @return
     */
    Builder removeInboundProperty(String key);

    /**
     * @param key
     * @return
     */
    Builder removeOutboundProperty(String key);

    /**
     * @deprecated use {@link DefaultMultiPartPayload} instead.
     * @param key
     * @param value
     * @return
     */
    @Deprecated
    Builder addInboundAttachment(String key, DataHandler value);

    /**
     * @deprecated use {@link DefaultMultiPartPayload} instead.
     * @param key
     * @param value
     * @return
     */
    @Deprecated
    Builder addOutboundAttachment(String key, DataHandler value);

    /**
     * @deprecated use {@link DefaultMultiPartPayload} instead.
     * @param key
     * @return
     */
    @Deprecated
    Builder removeInboundAttachment(String key);

    /**
     * @deprecated use {@link DefaultMultiPartPayload} instead.
     * @param key
     * @return
     */
    @Deprecated
    Builder removeOutboundAttachment(String key);

    /**
     * @param inboundProperties
     * @return
     */
    Builder inboundProperties(Map<String, Serializable> inboundProperties);

    /**
     * @param outboundProperties
     * @return
     */
    Builder outboundProperties(Map<String, Serializable> outboundProperties);

    /**
     * @deprecated use {@link DefaultMultiPartPayload} instead.
     * @param inboundAttachments
     * @return
     */
    @Deprecated
    Builder inboundAttachments(Map<String, DataHandler> inboundAttachments);

    /**
     * @deprecated use {@link DefaultMultiPartPayload} instead.
     * @param outbundAttachments
     * @return
     */
    @Deprecated
    Builder outboundAttachments(Map<String, DataHandler> outbundAttachments);

    @Override
    MuleMessage build();
  }

  interface CollectionBuilder extends org.mule.runtime.api.message.MuleMessage.CollectionBuilder, Builder {

    @Override
    CollectionBuilder itemMediaType(MediaType mediaType);

  }
}
