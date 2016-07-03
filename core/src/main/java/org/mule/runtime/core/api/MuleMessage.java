/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.message.DefaultMuleMessageBuilderFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import javax.activation.DataHandler;

/**
 * MuleMessage
 */
public interface MuleMessage<PAYLOAD, ATTRIBUTES extends Serializable> extends org.mule.runtime.api.message
        .MuleMessage<PAYLOAD, ATTRIBUTES>, MessageProperties, MessageAttachments, MessageTransform
{

    /**
     * Provides a builder to create {@link MuleMessage} objects.
     *
     * @return a new {@link Builder}.
     */
    static <PAYLOAD, ATTRIBUTES extends Serializable> PayloadBuilder<PAYLOAD, ATTRIBUTES> builder()
    {
        return DefaultMuleMessageBuilderFactory.getInstance().create();
    }

    /**
     * Provides a builder to create {@link MuleMessage} objects based on an existing {@link MuleMessage} instance.
     *
     * @param message existing {@link MuleMessage} to use as a template to create a new {@link Builder} instance.
     * @return a new {@link Builder} based on the template {@code message} provided.
     */
    static <PAYLOAD, ATTRIBUTES extends Serializable> Builder<PAYLOAD, ATTRIBUTES> builder(MuleMessage<PAYLOAD,
            ATTRIBUTES> message)
    {
        return DefaultMuleMessageBuilderFactory.getInstance().create(message);
    }

    static <PAYLOAD, ATTRIBUTES extends Serializable> Builder<PAYLOAD, ATTRIBUTES> builder(org.mule.runtime.api
                                                                                                   .message
                                                                                                   .MuleMessage<PAYLOAD, ATTRIBUTES> message)
    {
        return DefaultMuleMessageBuilderFactory.getInstance().create(message);
    }

    /**
     * gets the unique identifier for the message. It's up to the implementation to
     * ensure a unique id
     *
     * @return a unique message id. The Id should never be null. If the underlying
     * transport does not have the notion of a message Id, one should be
     * generated. The generated Id should be a UUID.
     */
    String getUniqueId();

    /**
     * gets an identifier that is the same among parent and child messages
     *
     * @return a message id for the group of descendant messages. The Id should never be null.
     */
    String getMessageRootId();

    /**
     * Sets a correlationId for this message. The correlation Id can be used by
     * components in the system to manage message relations. <p/> The correlationId
     * is associated with the message using the underlying transport protocol. As
     * such not all messages will support the notion of a correlationId i.e. tcp or
     * file. In this situation the correlation Id is set as a property of the message
     * where it's up to developer to keep the association with the message. For
     * example if the message is serialised to xml the correlationId will be
     * available in the message.
     *
     * @return the correlationId for this message or null if one hasn't been set
     */
    String getCorrelationId();

    /**
     * Gets the sequence or ordering number for this message in the the correlation
     * group (as defined by the correlationId)
     *
     * @return the sequence number or -1 if the sequence is not important
     */
    int getCorrelationSequence();

    /**
     * Determines how many messages are in the correlation group
     *
     * @return total messages in this group or -1 if the size is not known
     */
    int getCorrelationGroupSize();

    /**
     * Returns a replyTo address for this message. This is useful in an asynchronous
     * environment where the caller doesn't wait for a response and the response
     * needs to be routed somewhere for further processing. The value of this field
     * can be any valid endpointUri url.
     *
     * @return the endpointUri url to reply to or null if one has not been set
     */
    Object getReplyTo();

    /**
     * If an error occurred during the processing of this message this will return a
     * ErrorPayload that contains the root exception and Mule error code, plus any
     * other releated info
     *
     * @return The exception payload (if any) attached to this message
     */
    ExceptionPayload getExceptionPayload();

    interface PayloadBuilder<PAYLOAD, ATTRIBUTES extends Serializable> extends org.mule.runtime.api.message
            .MuleMessage.PayloadBuilder<PAYLOAD, ATTRIBUTES>
    {

        @Override
        <N> Builder<N, ATTRIBUTES> payload(N payload);

        @Override
        <N extends Collection<E>, E> CollectionBuilder<N, ATTRIBUTES> collectionPayload(N payload, Class<E> itemType);
    }

    interface Builder<PAYLOAD, ATTRIBUTES extends Serializable> extends org.mule.runtime.api.message.MuleMessage
            .Builder<PAYLOAD, ATTRIBUTES>, PayloadBuilder<PAYLOAD, ATTRIBUTES>
    {

        @Override
        Builder<PAYLOAD, ATTRIBUTES> mediaType(MediaType mediaType);

        @Override
        <N extends Serializable> Builder<PAYLOAD, N> attributes(N value);

        /**
         * @param correlationId
         * @return this builder.
         */
        Builder<PAYLOAD, ATTRIBUTES> correlationId(String correlationId);

        /**
         * @param correlationSequence
         * @return this builder.
         */
        Builder<PAYLOAD, ATTRIBUTES> correlationSequence(int correlationSequence);

        /**
         * @param correlationGroupSize
         * @return this builder.
         */
        Builder<PAYLOAD, ATTRIBUTES> correlationGroupSize(int correlationGroupSize);

        /**
         * @param exceptionPayload
         * @return this builder.
         */
        Builder<PAYLOAD, ATTRIBUTES> exceptionPayload(ExceptionPayload exceptionPayload);

        /**
         * @param replyTo
         * @return
         */
        Builder<PAYLOAD, ATTRIBUTES> replyTo(Object replyTo);

        /**
         * @param rootId
         * @return
         */
        Builder<PAYLOAD, ATTRIBUTES> rootId(String rootId);

        /**
         * @param key
         * @param value
         * @return
         */
        Builder<PAYLOAD, ATTRIBUTES> addInboundProperty(String key, Serializable value);

        /**
         * @param key
         * @param value
         * @param mediaType
         * @param <T>
         * @return
         */
        <T extends Serializable> Builder<PAYLOAD, ATTRIBUTES> addInboundProperty(String key, T value, MediaType mediaType);

        /**
         * @param key
         * @param value
         * @param dataType
         * @param <T>
         * @return
         */
        <T extends Serializable> Builder<PAYLOAD, ATTRIBUTES> addInboundProperty(String key, T value, DataType<T> dataType);

        /**
         * @param key
         * @param value
         * @return
         */
        Builder<PAYLOAD, ATTRIBUTES> addOutboundProperty(String key, Serializable value);

        /**
         * @param key
         * @param value
         * @param mediaType
         * @param <T>
         * @return
         */
        <T extends Serializable> Builder<PAYLOAD, ATTRIBUTES> addOutboundProperty(String key, T value, MediaType mediaType);

        /**
         * @param key
         * @param value
         * @param dataType
         * @param <T>
         * @return
         */
        <T extends Serializable> Builder<PAYLOAD, ATTRIBUTES> addOutboundProperty(String key, T value, DataType<T> dataType);

        /**
         * @param key
         * @return
         */
        Builder<PAYLOAD, ATTRIBUTES> removeOutboundProperty(String key);

        /**
         * @return
         */
        Builder<PAYLOAD, ATTRIBUTES> clearOutboundProperties();

        /**
         * @param key
         * @param value
         * @return
         */
        Builder<PAYLOAD, ATTRIBUTES> addInboundAttachment(String key, DataHandler value);

        /**
         * @param key
         * @param value
         * @return
         */
        Builder<PAYLOAD, ATTRIBUTES> addOutboundAttachment(String key, DataHandler value);

        /**
         * @param key
         * @param value
         * @return
         */
        Builder<PAYLOAD, ATTRIBUTES> removeInboundAttachment(String key);

        /**
         * @param key
         * @param value
         * @return
         */
        Builder<PAYLOAD, ATTRIBUTES> removeOutboundAttachment(String key);

        /**
         * Removes all outbound attachments on this message builder.
         * 
         * @return
         */
        Builder<PAYLOAD, ATTRIBUTES> clearOutbloundAttachments();

        /**
         * @param inboundProperties
         * @return
         */
        Builder<PAYLOAD, ATTRIBUTES> inboundProperties(Map<String, Serializable> inboundProperties);

        /**
         * @param outboundProperties
         * @return
         */
        Builder<PAYLOAD, ATTRIBUTES> outboundProperties(Map<String, Serializable> outboundProperties);

        /**
         * @param inboundAttachments
         * @return
         */
        Builder<PAYLOAD, ATTRIBUTES> inboundAttachments(Map<String, DataHandler> inboundAttachments);

        /**
         * @param outbundAttachments
         * @return
         */
        Builder<PAYLOAD, ATTRIBUTES> outboundAttachments(Map<String, DataHandler> outbundAttachments);

        @Override
        MuleMessage<PAYLOAD, ATTRIBUTES> build();
    }

    interface CollectionBuilder<PAYLOAD, ATTRIBUTES extends Serializable> extends org.mule.runtime.api.message.MuleMessage
            .CollectionBuilder<PAYLOAD, ATTRIBUTES>, Builder<PAYLOAD, ATTRIBUTES>
    {
        @Override
        CollectionBuilder<PAYLOAD, ATTRIBUTES> itemMediaType(MediaType mediaType);

    }
}
