/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Objects.requireNonNull;
import static org.mule.runtime.core.api.config.MuleProperties.CONTENT_TYPE_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_ID_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ENCODING_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.ExceptionPayload;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.api.MuleMessage.CollectionBuilder;
import org.mule.runtime.core.message.NullAttributes;
import org.mule.runtime.core.metadata.DefaultCollectionDataType;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.util.CaseInsensitiveMapWrapper;
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
public class DefaultMuleMessageBuilder implements MuleMessage.Builder, MuleMessage.PayloadBuilder, MuleMessage.CollectionBuilder
{

    private static final Logger logger = LoggerFactory.getLogger(DefaultMuleMessageBuilder.class);

    private Object payload;
    private DataType dataType;
    private Attributes attributes = new NullAttributes();

    private String id;
    private String rootId;
    private String correlationId;
    private Integer correlationSequence;
    private Integer correlationGroupSize;
    private Object replyTo;
    private ExceptionPayload exceptionPayload;

    private Map<String, TypedValue<Serializable>> inboundProperties = new CaseInsensitiveMapWrapper<>(HashMap.class);
    private Map<String, TypedValue<Serializable>> outboundProperties = new CaseInsensitiveMapWrapper<>(HashMap.class);
    private Map<String, DataHandler> inboundAttachments = new HashMap<>();
    private Map<String, DataHandler> outboundAttachments = new HashMap<>();

    public DefaultMuleMessageBuilder()
    {
    }

    public DefaultMuleMessageBuilder(MuleMessage message)
    {
        this((org.mule.runtime.api.message.MuleMessage) message);
    }

    private void copyMessageAttributes(MuleMessage message)
    {
        this.id = message.getUniqueId();
        this.correlationId = message.getCorrelationId();
        this.correlationSequence = message.getCorrelationSequence();
        this.correlationGroupSize = message.getCorrelationGroupSize();
        this.replyTo = message.getReplyTo();
        this.rootId = message.getMessageRootId();
        this.exceptionPayload = message.getExceptionPayload();
        message.getInboundPropertyNames().forEach(key -> {
            if (message.getInboundPropertyDataType(key) != null)
            {
                addInboundProperty(key, message.getInboundProperty(key), message.getInboundPropertyDataType(key));
            }
            else
            {
                addInboundProperty(key, message.getInboundProperty(key));
            }
        });
        message.getOutboundPropertyNames().forEach(key -> {
            if (message.getOutboundPropertyDataType(key) != null)
            {
                addOutboundProperty(key, message.getOutboundProperty(key), message.getOutboundPropertyDataType(key));
            }
            else
            {
                addOutboundProperty(key, message.getOutboundProperty(key));
            }
        });
        message.getInboundAttachmentNames().forEach(name -> addInboundAttachment(name, message.getInboundAttachment(name)));
        message.getOutboundAttachmentNames().forEach(name -> addOutboundAttachment(name, message.getOutboundAttachment(name)));
    }

    public DefaultMuleMessageBuilder(org.mule.runtime.api.message.MuleMessage message)
    {
        this.payload = message.getPayload();
        this.dataType = message.getDataType();
        this.attributes = message.getAttributes();

        if (message instanceof MuleMessage)
        {
            copyMessageAttributes((MuleMessage) message);
        }
    }

    @Override
    public Builder payload(Object payload)
    {
        requireNonNull(payload);
        this.payload = payload;
        return this;
    }

    @Override
    public MuleMessage.CollectionBuilder collectionPayload(Collection payload, Class<?> clazz)
    {
        this.payload = payload;
        this.dataType = DataType.builder().collectionType(payload.getClass()).itemType(clazz).build();
        return this;
    }

    @Override
    public CollectionBuilder itemMediaType(MediaType mediaType)
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
    public Builder mediaType(MediaType mediaType)
    {
        this.dataType = DataType.builder().mediaType(mediaType).build();
        return this;
    }

    @Override
    public MuleMessage.Builder attributes(Attributes attributes)
    {
        this.attributes = attributes;
        return this;
    }

    @Override
    public Builder correlationId(String correlationId)
    {
        this.correlationId = correlationId;
        return this;
    }

    @Override
    public Builder correlationSequence(Integer correlationSequence)
    {
        this.correlationSequence = correlationSequence;
        return this;
    }

    @Override
    public Builder correlationGroupSize(Integer correlationGroupSize)
    {
        this.correlationGroupSize = correlationGroupSize;
        return this;
    }

    @Override
    public Builder exceptionPayload(ExceptionPayload exceptionPayload)
    {
        this.exceptionPayload = exceptionPayload;
        return this;
    }

    @Override
    public Builder replyTo(Object replyTo)
    {
        this.replyTo = replyTo;
        return this;
    }

    @Override
    public Builder id(String id)
    {
        this.id = id;
        return this;
    }

    @Override
    public Builder rootId(String rootId)
    {
        this.rootId = rootId;
        return this;
    }

    @Override
    public Builder addInboundProperty(String key, Serializable value)
    {
        inboundProperties.put(key, new TypedValue(value, value != null ? DataType.fromObject(value) : DataType.OBJECT));
        updateDataTypeWithProperty(key, value);
        return this;
    }

    @Override
    public Builder addInboundProperty(String key, Serializable value, MediaType mediaType)
    {
        inboundProperties.put(key, new TypedValue(value, DataType.builder().type(value.getClass()).mediaType(mediaType).build()));
        updateDataTypeWithProperty(key, value);
        return this;
    }

    @Override
    public Builder addInboundProperty(String key, Serializable value, DataType dataType)
    {
        inboundProperties.put(key, new TypedValue(value, dataType));
        updateDataTypeWithProperty(key, value);
        return this;
    }

    @Override
    public Builder addOutboundProperty(String key, Serializable value)
    {
        outboundProperties.put(key, new TypedValue(value, value != null ? DataType.fromObject(value) : DataType.OBJECT));
        updateDataTypeWithProperty(key, value);
        return this;
    }

    @Override
    public Builder addOutboundProperty(String key, Serializable value, MediaType mediaType)
    {
        outboundProperties.put(key, new TypedValue(value, DataType.builder().type(value.getClass()).mediaType(mediaType).build()));
        updateDataTypeWithProperty(key, value);
        return this;
    }

    @Override
    public Builder addOutboundProperty(String key, Serializable value, DataType dataType)
    {
        outboundProperties.put(key, new TypedValue(value, dataType));
        updateDataTypeWithProperty(key, value);
        return this;
    }

    @Override
    public Builder removeInboundProperty(String key)
    {
        inboundProperties.remove(key);
        return this;
    }

    @Override
    public Builder removeOutboundProperty(String key)
    {
        outboundProperties.remove(key);
        return this;
    }

    @Override
    public Builder addInboundAttachment(String key, DataHandler value)
    {
        inboundAttachments.put(key, value);
        return this;
    }

    @Override
    public Builder addOutboundAttachment(String key, DataHandler value)
    {
        outboundAttachments.put(key, value);
        return this;
    }

    @Override
    public Builder removeInboundAttachment(String key)
    {
        inboundAttachments.remove(key);
        return this;
    }

    @Override
    public Builder removeOutboundAttachment(String key)
    {
        outboundAttachments.remove(key);
        return this;
    }

    @Override
    public Builder inboundProperties(Map<String, Serializable> inboundProperties)
    {
        requireNonNull(inboundProperties);
        this.inboundProperties.clear();
        inboundProperties.forEach((s, serializable) -> addInboundProperty(s, serializable));
        return this;
    }

    @Override
    public Builder outboundProperties(Map<String, Serializable> outboundProperties)
    {
        requireNonNull(outboundProperties);
        this.outboundProperties.clear();
        outboundProperties.forEach((s, serializable) -> addOutboundProperty(s, serializable));
        return this;
    }

    @Override
    public Builder inboundAttachments(Map<String, DataHandler> inboundAttachments)
    {
        requireNonNull(inboundAttachments);
        this.inboundAttachments = new HashMap<>(inboundAttachments);
        return this;
    }

    @Override
    public Builder outboundAttachments(Map<String, DataHandler> outbundAttachments)
    {
        requireNonNull(outbundAttachments);
        this.outboundAttachments = new HashMap<>(outbundAttachments);
        return this;
    }

    @Override
    public MuleMessage build()
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

    // TODO MULE-9858 remove this magic properties
    @Deprecated
    private void updateDataTypeWithProperty(String key, Object value)
    {
        // updates dataType when encoding is updated using a property instead of using #setEncoding
        if (MULE_ENCODING_PROPERTY.equals(key))
        {
            final Class type = dataType != null ? dataType.getType() : Object.class;
            dataType = DataType.builder().type(type).charset((String) value).build();
        }
        else if (CONTENT_TYPE_PROPERTY.equalsIgnoreCase(key))
        {
            final DataTypeBuilder builder = DataType.builder();
            try
            {
                builder.mediaType((String) value);
            }
            catch (IllegalArgumentException e)
            {
                if (Boolean.parseBoolean(System.getProperty(SYSTEM_PROPERTY_PREFIX + "strictContentType")))
                {
                    throw new IllegalArgumentException("Invalid Content-Type property value", e);
                }
                else
                {
                    String encoding = defaultCharset().name();
                    logger.warn(format("%s when parsing Content-Type '%s': %s", e.getClass().getName(), value, e.getMessage()));
                    logger.warn(format("Using defualt encoding: %s", encoding));
                    builder.charset(encoding);
                }
            }
            final Class type = dataType != null ? dataType.getType() : Object.class;
            dataType = builder.type(type).build();
        }
        else if (MULE_CORRELATION_ID_PROPERTY.equalsIgnoreCase(key))
        {
            correlationId = value.toString();
        }
        else if ("MULE_REPLYTO".equalsIgnoreCase(key))
        {
            if(replyTo == null)
            {
                replyTo = value;
            }
        }
    }

}
