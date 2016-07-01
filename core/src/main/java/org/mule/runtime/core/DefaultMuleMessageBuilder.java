/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.util.Objects.requireNonNull;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.ExceptionPayload;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.CollectionBuilder;
import org.mule.runtime.core.metadata.DefaultCollectionDataType;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.util.UUID;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * // TODO MULE-9855 MOVE TO org.mule.runtime.core.message
 */
public class DefaultMuleMessageBuilder<PAYLOAD, ATTRIBUTES extends Serializable> implements MuleMessage
        .Builder<PAYLOAD, ATTRIBUTES>, MuleMessage.PayloadBuilder<PAYLOAD, ATTRIBUTES>, MuleMessage.CollectionBuilder<PAYLOAD, ATTRIBUTES>
{

    private static final Logger logger = LoggerFactory.getLogger(DefaultMuleMessageBuilder.class);

    private PAYLOAD payload;
    private DataType<PAYLOAD> dataType;
    private ATTRIBUTES attributes;

    private String id;
    private String rootId;
    private String correlationId;
    private int correlationSequence;
    private int correlationGroupSize;
    private Object replyTo;
    private ExceptionPayload exceptionPayload;

    private Map<String, TypedValue<Serializable>> inboundProperties = new HashMap<>();
    private Map<String, TypedValue<Serializable>> outboundProperties = new HashMap<>();
    private Map<String, DataHandler> inboundAttachments = new HashMap<>();
    private Map<String, DataHandler> outboundAttachments = new HashMap<>();

    public DefaultMuleMessageBuilder()
    {
    }

    public DefaultMuleMessageBuilder(MuleMessage<PAYLOAD, ATTRIBUTES> message)
    {
        this((org.mule.runtime.api.message.MuleMessage) message);
        this.id = message.getUniqueId();
        this.correlationId = message.getCorrelationId();
        this.correlationSequence = message.getCorrelationSequence();
        this.correlationGroupSize = message.getCorrelationGroupSize();
        this.replyTo = message.getReplyTo();
        this.rootId = message.getMessageRootId();
        this.exceptionPayload = message.getExceptionPayload();
        message.getInboundPropertyNames().forEach(key -> {
            inboundProperties.put(key, new TypedValue(message.getInboundProperty(key), message
                                                                                               .getInboundPropertyDataType(key) != null ? message
                                                                                               .getInboundPropertyDataType(key) : DataType.OBJECT));
        });
        message.getOutboundPropertyNames().forEach(key -> {
            outboundProperties.put(key, new TypedValue(message.getOutboundProperty(key), message
                                                                                                 .getOutboundPropertyDataType(key) != null ? message
                                                                                                 .getOutboundPropertyDataType(key) : DataType.OBJECT));
        });
    }

    public DefaultMuleMessageBuilder(org.mule.runtime.api.message.MuleMessage<PAYLOAD, ATTRIBUTES> message)
    {
        this.payload = message.getPayload();
        this.dataType = message.getDataType();
        this.attributes = message.getAttributes();
    }

    @Override
    public <N> MuleMessage.Builder<N, ATTRIBUTES> payload(N payload)
    {
        requireNonNull(payload);
        this.payload = (PAYLOAD) payload;
        return (MuleMessage.Builder<N, ATTRIBUTES>) this;
    }

    @Override
    public <N extends Collection<E>, E> MuleMessage.CollectionBuilder<N, ATTRIBUTES>
    collectionPayload(N payload, Class<E> clazz)
    {
        this.payload = (PAYLOAD) payload;
        this.dataType = DataType.builder().collectionType(payload.getClass()).itemType(clazz).build();
        return (MuleMessage.CollectionBuilder<N, ATTRIBUTES>) this;
    }

    @Override
    public CollectionBuilder<PAYLOAD, ATTRIBUTES> itemMediaType(MediaType mediaType)
    {
        if (dataType instanceof DefaultCollectionDataType)
        {
            dataType = ((DataTypeBuilder.DataTypeCollectionTypeBuilder) DataType.builder(this.dataType)).itemMediaType(mediaType).build();
        }
        else
        {
            throw new IllegalStateException("Item MediaType cannot be set, because payload is not a collection");
        }
        return this;
    }

    @Override
    public MuleMessage.Builder<PAYLOAD, ATTRIBUTES> mediaType(MediaType mediaType)
    {
        this.dataType = DataType.<PAYLOAD>builder().mediaType(mediaType).build();
        return this;
    }

    @Override
    public <N extends Serializable> MuleMessage.Builder<PAYLOAD, N> attributes(N attributes)
    {
        this.attributes = (ATTRIBUTES) attributes;
        return (MuleMessage.Builder<PAYLOAD, N>) this;
    }

    @Override
    public MuleMessage.Builder<PAYLOAD, ATTRIBUTES> correlationId(String correlationId)
    {
        this.correlationId = correlationId;
        return this;
    }

    @Override
    public MuleMessage.Builder<PAYLOAD, ATTRIBUTES> correlationSequence(int correlationSequence)
    {
        this.correlationSequence = correlationSequence;
        return this;
    }

    @Override
    public MuleMessage.Builder<PAYLOAD, ATTRIBUTES> correlationGroupSize(int correlationGroupSize)
    {
        this.correlationGroupSize = correlationGroupSize;
        return this;
    }

    @Override
    public MuleMessage.Builder<PAYLOAD, ATTRIBUTES> exceptionPayload(ExceptionPayload exceptionPayload)
    {
        this.exceptionPayload = exceptionPayload;
        return this;
    }

    @Override
    public MuleMessage.Builder<PAYLOAD, ATTRIBUTES> replyTo(Object replyTo)
    {
        this.replyTo = replyTo;
        return this;
    }

    @Override
    public MuleMessage.Builder<PAYLOAD, ATTRIBUTES> rootId(String rootId)
    {
        this.rootId = rootId;
        return this;
    }

    @Override
    public MuleMessage.Builder<PAYLOAD, ATTRIBUTES> addInboundProperty(String key, Serializable value)
    {
        inboundProperties.put(key, new TypedValue(value, DataType.fromObject(value)));
        return this;
    }

    @Override
    public <T extends Serializable> MuleMessage.Builder<PAYLOAD, ATTRIBUTES> addInboundProperty(String key, T value, MediaType mediaType)
    {
        inboundProperties.put(key, new TypedValue(value, DataType.builder().type(value.getClass()).mediaType(mediaType).build()));
        return this;
    }

    @Override
    public <T extends Serializable> MuleMessage.Builder<PAYLOAD, ATTRIBUTES> addInboundProperty(String key, T value, DataType<T>
            dataType)
    {
        inboundProperties.put(key, new TypedValue(value, dataType));
        return this;
    }

    @Override
    public MuleMessage.Builder<PAYLOAD, ATTRIBUTES> addOutboundProperty(String key, Serializable value)
    {
        outboundProperties.put(key, new TypedValue(value, DataType.fromObject(value)));
        return this;
    }

    @Override
    public <T extends Serializable> MuleMessage.Builder<PAYLOAD, ATTRIBUTES> addOutboundProperty(String key, T value, MediaType mediaType)
    {
        outboundProperties.put(key, new TypedValue(value, DataType.builder().type(value.getClass()).mediaType(mediaType).build()));
        return this;
    }

    @Override
    public <T extends Serializable> MuleMessage.Builder<PAYLOAD, ATTRIBUTES> addOutboundProperty(String key, T value, DataType<T>
            dataType)
    {
        outboundProperties.put(key, new TypedValue(value, dataType));
        return this;
    }

    @Override
    public MuleMessage.Builder<PAYLOAD, ATTRIBUTES> addOutboundAttachement(String key, DataHandler value)
    {
        outboundAttachments.put(key, value);
        return this;
    }

    @Override
    public MuleMessage.Builder<PAYLOAD, ATTRIBUTES> inboundProperties(Map<String, Serializable> inboundProperties)
    {
        this.inboundProperties.clear();
        inboundProperties.forEach((s, serializable) -> addInboundProperty(s, serializable));
        return this;
    }

    @Override
    public MuleMessage.Builder<PAYLOAD, ATTRIBUTES> outboundProperties(Map<String, Serializable> outboundProperties)
    {
        this.outboundProperties.clear();
        outboundProperties.forEach((s, serializable) -> addOutboundProperty(s, serializable));
        return this;
    }

    @Override
    public MuleMessage.Builder<PAYLOAD, ATTRIBUTES> inboundAttachements(Map<String, DataHandler> inboundAttachments)
    {
        this.inboundAttachments = inboundAttachments;
        return this;
    }

    @Override
    public MuleMessage.Builder<PAYLOAD, ATTRIBUTES> outboundAttachements(Map<String, DataHandler> outbundAttachments)
    {
        this.outboundAttachments = outbundAttachments;
        return this;
    }

    @Override
    public MuleMessage<PAYLOAD, ATTRIBUTES> build()
    {
        return new DefaultMuleMessage(id != null ? id : UUID.getUUID(), rootId,
                                                         new TypedValue(payload, resolveDataType()), attributes,
                                                         inboundProperties, outboundProperties, inboundAttachments,
                                                         outboundAttachments, correlationId, correlationGroupSize,
                                                         correlationSequence, replyTo, exceptionPayload);
    }

    private DataType resolveDataType()
    {
        if (dataType == null)
        {
            return DataType.fromObject(payload);
        }
        else
        {
            return DataType.builder(dataType).fromObject(payload).build();
        }
    }

}
