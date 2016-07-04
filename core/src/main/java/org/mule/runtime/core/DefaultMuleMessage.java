/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;
import static org.mule.runtime.core.api.config.MuleProperties.CONTENT_TYPE_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_ID_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ENCODING_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.util.ObjectUtils.getInt;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.core.api.ExceptionPayload;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.runtime.core.util.StringMessageUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.activation.DataHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>DefaultMuleMessage</code> is a wrapper that contains a payload and properties
 * associated with the payload.
 */
public class DefaultMuleMessage implements MuleMessage, DeserializationPostInitialisable
{
    private static final String NOT_SET = "<not set>";

    private static final long serialVersionUID = 1541720810851984845L;
    private static final Logger logger = LoggerFactory.getLogger(DefaultMuleMessage.class);

    /**
     * The default UUID for the message. If the underlying transport has the notion of a
     * message id, this uuid will be ignored
     */
    private String id;
    private String rootId;

    /**
     * If an exception occurs while processing this message an exception payload
     * will be attached here
     */
    private ExceptionPayload exceptionPayload;

    /**
     * Scoped properties for this message
     */
    private MessagePropertiesContext properties = new MessagePropertiesContext();

    /**
     * Collection of attachments that were attached to the incoming message
     */
    private transient Map<String, DataHandler> inboundAttachments = new HashMap<>();

    /**
     * Collection of attachments that will be sent out with this message
     */
    private transient Map<String, DataHandler> outboundAttachments = new HashMap<>();

    // these are transient because serialisation generates a new instance
    // so we allow mutation again (and we can't serialize threads anyway)
    private transient AtomicReference<Thread> ownerThread = null;
    private transient AtomicBoolean mutable = null;

    private transient TypedValue typedValue;
    private Serializable attributes;

    DefaultMuleMessage(String id, String rootId, TypedValue typedValue, Serializable attributes,
                       Map<String, TypedValue<Serializable>> inboundProperties,
                       Map<String, TypedValue<Serializable>> outboundProperties,
                       Map<String, DataHandler> inboundAttachments, Map<String, DataHandler> outboundAttachments,
                       String corealationId, Integer correlationGroupSize, Integer correlationSequence,
                       Object replyTo, ExceptionPayload exceptionPayload)
    {
        this.id = id;
        this.rootId = rootId != null ? rootId : id;
        this.typedValue = typedValue;
        this.attributes = attributes;
        this.properties.inboundMap.putAll(inboundProperties);
        this.properties.outboundMap.putAll(outboundProperties);
        this.inboundAttachments = inboundAttachments;
        this.outboundAttachments = outboundAttachments;
        setCorrelationId(corealationId);
        setCorrelationGroupSize(correlationGroupSize);
        setCorrelationSequence(correlationSequence);
        setReplyTo(replyTo);
        this.exceptionPayload = exceptionPayload;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUniqueId()
    {
        return id;
    }

    @Override
    public String getMessageRootId()
    {
        return rootId;
    }

    private void setCorrelationId(String id)
    {
        if (StringUtils.isNotBlank(id))
        {
            setOutboundProperty(MULE_CORRELATION_ID_PROPERTY, id, DataType.STRING);
        }
        else
        {
            removeOutboundProperty(MULE_CORRELATION_ID_PROPERTY);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCorrelationId()
    {
        String correlationId = (String) getOutboundProperty(MULE_CORRELATION_ID_PROPERTY);
        if (correlationId == null)
        {
            correlationId = (String) getInboundProperty(MULE_CORRELATION_ID_PROPERTY);
        }

        return correlationId;
    }

    private void setReplyTo(Object replyTo)
    {
        if (replyTo != null)
        {
            if(!(replyTo instanceof Serializable))
            {
                logger.warn("ReplyTo " + replyTo + " is not serializable and will not be propagated by Mule");
            }
            setOutboundProperty(MULE_REPLY_TO_PROPERTY, (Serializable) replyTo);
        }
        else
        {
            removeOutboundProperty(MULE_REPLY_TO_PROPERTY);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getReplyTo()
    {
        Serializable replyTo = getOutboundProperty(MULE_REPLY_TO_PROPERTY);
        if (replyTo == null)
        {
            // fallback to inbound, use the requestor's setting if the invocation didn't set any
            replyTo = getInboundProperty(MULE_REPLY_TO_PROPERTY);
        }
        return replyTo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getCorrelationSequence()
    {
        // need to wrap with another getInt() as some transports operate on it as a String
        final int correlationSequence = getInt(findProperty(MULE_CORRELATION_SEQUENCE_PROPERTY), -1);
        return correlationSequence < 0 ? null : correlationSequence;
    }

    private void setCorrelationSequence(Integer sequence)
    {
        if (sequence != null)
        {
            setOutboundProperty(MULE_CORRELATION_SEQUENCE_PROPERTY, sequence);
        }
        else
        {
            removeOutboundProperty(MULE_CORRELATION_SEQUENCE_PROPERTY);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getCorrelationGroupSize()
    {
        // need to wrap with another getInt() as some transports operate on it as a String
        final int correlationGroupSize = getInt(findProperty(MULE_CORRELATION_GROUP_SIZE_PROPERTY), -1);
        return correlationGroupSize < 0 ? null : correlationGroupSize;
    }

    private void setCorrelationGroupSize(Integer size)
    {
        if (size != null)
        {
            setOutboundProperty(MULE_CORRELATION_GROUP_SIZE_PROPERTY, size);
        }
        else
        {
            removeOutboundProperty(MULE_CORRELATION_GROUP_SIZE_PROPERTY);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExceptionPayload getExceptionPayload()
    {
        return exceptionPayload;
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder(120);

        // format message for multi-line output, single-line is not readable
        buf.append(LINE_SEPARATOR);
        buf.append(getClass().getName());
        buf.append(LINE_SEPARATOR);
        buf.append("{");
        buf.append(LINE_SEPARATOR);
        buf.append("  id=").append(getUniqueId());
        buf.append(LINE_SEPARATOR);
        buf.append("  payload=").append(getPayload().getClass().getName());
        buf.append(LINE_SEPARATOR);
        buf.append("  correlationId=").append(StringUtils.defaultString(getCorrelationId(), NOT_SET));
        buf.append(LINE_SEPARATOR);
        buf.append("  correlationGroup=").append(getCorrelationGroupSize());
        buf.append(LINE_SEPARATOR);
        buf.append("  correlationSeq=").append(getCorrelationSequence());
        buf.append(LINE_SEPARATOR);
        buf.append("  exceptionPayload=").append(ObjectUtils.defaultIfNull(exceptionPayload, NOT_SET));
        buf.append(LINE_SEPARATOR);
        buf.append(StringMessageUtils.headersToString(this));
        // no new line here, as headersToString() adds one
        buf.append('}');
        return buf.toString();
    }

    @Override
    public DataHandler getInboundAttachment(String name)
    {
        return inboundAttachments.get(name);
    }

    @Override
    public DataHandler getOutboundAttachment(String name)
    {
        return outboundAttachments.get(name);
    }

    @Override
    public Set<String> getInboundAttachmentNames()
    {
        return unmodifiableSet(inboundAttachments.keySet());
    }

    @Override
    public Set<String> getOutboundAttachmentNames()
    {
        return unmodifiableSet(outboundAttachments.keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getPayload()
    {
        return typedValue.getValue();
    }

    private void setDataType(DataType dt)
    {
        typedValue = new TypedValue(typedValue.getValue(), dt);
    }


    private IllegalStateException newException(String message)
    {
        IllegalStateException exception = new IllegalStateException(message);
        logger.warn("Message access violation", exception);
        return exception;
    }

    public static class SerializedDataHandler implements Serializable
    {
        private static final long serialVersionUID = 1L;

        private DataHandler handler;
        private String contentType;
        private Object contents;

        public SerializedDataHandler(String name, DataHandler handler, MuleContext muleContext) throws IOException
        {
            if (handler != null && !(handler instanceof Serializable))
            {
                contentType = handler.getContentType();
                Object theContent = handler.getContent();
                if (theContent instanceof Serializable)
                {
                    contents = theContent;
                }
                else
                {
                    try
                    {
                        DataType source = DataType.fromObject(theContent);
                        Transformer transformer = muleContext.getRegistry().lookupTransformer(source, DataType.BYTE_ARRAY);
                        if (transformer == null)
                        {
                            throw new TransformerException(CoreMessages.noTransformerFoundForMessage(source, DataType.BYTE_ARRAY));
                        }
                        contents = transformer.transform(theContent);
                    }
                    catch(TransformerException ex)
                    {
                        String message = String.format(
                                "Unable to serialize the attachment %s, which is of type %s with contents of type %s",
                                name, handler.getClass(), theContent.getClass());
                        logger.error(message);
                        throw new IOException(message);
                    }
                }
            }
            else
            {
                this.handler = handler;
            }
        }

        public DataHandler getHandler()
        {
            return contents != null ? new DataHandler(contents, contentType) : handler;
        }
    }

    private void writeObject(ObjectOutputStream out) throws Exception
    {
        out.defaultWriteObject();
        serializeValue(out);
        out.writeObject(typedValue.getDataType());
        out.writeObject(serializeAttachments(inboundAttachments));
        out.writeObject(serializeAttachments(outboundAttachments));
    }

    private Map<String, SerializedDataHandler> serializeAttachments(Map<String, DataHandler> attachments) throws IOException
    {
        Map<String, SerializedDataHandler> toWrite;
        if (attachments == null)
        {
            toWrite = null;
        }
        else
        {
            toWrite = new HashMap<>(attachments.size());
            for (Map.Entry<String, DataHandler> entry : attachments.entrySet())
            {
                String name = entry.getKey();
                // TODO MULE-10013 remove this logic from here
                toWrite.put(name, new SerializedDataHandler(name, entry.getValue(), RequestContext.getEvent().getMuleContext()));
            }
        }

        return toWrite;
    }

    protected void serializeValue(ObjectOutputStream out) throws Exception
    {
        if (typedValue.getValue() instanceof Serializable)
        {
            out.writeBoolean(true);
            out.writeObject(typedValue.getValue());
        }
        else
        {
            out.writeBoolean(false);
            // TODO MULE-10013 remove this logic from here
            byte[] valueAsByteArray = (byte[]) RequestContext.getEvent().getMuleContext().getTransformationService().transform(this, DataType.BYTE_ARRAY).getPayload();
            out.writeInt(valueAsByteArray.length);
            new DataOutputStream(out).write(valueAsByteArray);
        }
    }

    protected Object deserializeValue(ObjectInputStream in) throws Exception
    {
        boolean valueSerialized = in.readBoolean();
        if (valueSerialized)
        {
            return in.readObject();
        }
        else
        {
            int length = in.readInt();
            byte[] valueAsByteArray = new byte[length];
            new DataInputStream(in).readFully(valueAsByteArray);
            return valueAsByteArray;
        }
    }

    private Map<String, DataHandler> deserializeAttachments(Map<String, SerializedDataHandler> attachments) throws IOException
    {
        Map<String, DataHandler> toReturn;
        if (attachments == null)
        {
            toReturn = emptyMap();
        }
        else
        {
            toReturn = new HashMap<>(attachments.size());
            for (Map.Entry<String, SerializedDataHandler> entry : attachments.entrySet())
            {
                toReturn.put(entry.getKey(), entry.getValue().getHandler());
            }
        }

        return toReturn;
    }

    private void readObject(ObjectInputStream in) throws Exception
    {
        in.defaultReadObject();
        typedValue = new TypedValue(deserializeValue(in), (DataType) in.readObject());
        inboundAttachments = deserializeAttachments((Map<String, SerializedDataHandler>)in.readObject());
        outboundAttachments = deserializeAttachments((Map<String, SerializedDataHandler>)in.readObject());
    }

    /**
     * Invoked after deserialization. This is called when the marker interface
     * {@link org.mule.runtime.core.util.store.DeserializationPostInitialisable} is used. This will get invoked after
     * the object has been deserialized passing in the current mulecontext when using either
     * {@link org.mule.runtime.core.transformer.wire.SerializationWireFormat},
     * {@link org.mule.runtime.core.transformer.wire.SerializedMuleMessageWireFormat} or the
     * {@link org.mule.runtime.core.transformer.simple.ByteArrayToSerializable} transformer.
     *
     * @param context the current muleContext instance
     * @throws MuleException if there is an error initializing
     */
    public void initAfterDeserialisation(MuleContext context) throws MuleException
    {
        if (this.inboundAttachments == null)
        {
            this.inboundAttachments = new HashMap<>();
        }

        if (this.outboundAttachments == null)
        {
            this.outboundAttachments = new HashMap<>();
        }
    }

    /**
     * Find property by searching outbound and then inbound scopes in order.
     * @param name name of the property to find
     * @return value of the property or null if property is not found in either scope
     */
    @SuppressWarnings("unchecked")
    private Serializable findProperty(String name)
    {
        Serializable result = getOutboundProperty(name);
        if (result != null)
        {
            return result;
        }
        else
        {
            return getInboundProperty(name);
        }
    }

    @Override
    public Serializable getAttributes()
    {
        return attributes;
    }

    @Override
    public DataType getDataType()
    {
        return typedValue.getDataType();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof DefaultMuleMessage))
        {
            return false;
        }
        return this.id.equals(((DefaultMuleMessage)obj).id);
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    @Override
    public Serializable getInboundProperty(String name)
    {
        return properties.getInboundProperty(name);
    }

    @Override
    public Serializable getInboundProperty(String name, Serializable defaultValue)
    {
        return properties.getInboundProperty(name, defaultValue);
    }

    @Override
    public Serializable getOutboundProperty(String name)
    {
        return properties.getOutboundProperty(name);
    }

    @Override
    public Serializable getOutboundProperty(String name, Serializable defaultValue)
    {
        return properties.getOutboundProperty(name, defaultValue);
    }

    public void setInboundProperty(String key, Serializable value, DataType dataType)
    {
        properties.setInboundProperty(key, value, dataType);
        updateDataTypeWithProperty(key, value);
    }

    public void setOutboundProperty(String key, Serializable value)
    {
        properties.setOutboundProperty(key, value);
        updateDataTypeWithProperty(key, value);
    }

    public void setOutboundProperty(String key, Serializable value, DataType dataType)
    {
        properties.setOutboundProperty(key, value, dataType);
        updateDataTypeWithProperty(key, value);
    }

    public Serializable removeOutboundProperty(String key)
    {
        return properties.removeOutboundProperty(key);
    }

    @Override
    public Set<String> getInboundPropertyNames()
    {
        return unmodifiableSet(properties.getInboundPropertyNames());
    }

    @Override
    public Set<String> getOutboundPropertyNames()
    {
        return properties.getOutboundPropertyNames();
    }

    @Override
    public DataType getInboundPropertyDataType(String name)
    {
        return properties.getInboundPropertyDataType(name);
    }

    @Override
    public DataType getOutboundPropertyDataType(String name)
    {
        return properties.getOutboundPropertyDataType(name);
    }

    private void updateDataTypeWithProperty(String key, Object value)
    {
        // updates dataType when encoding is updated using a property instead of using #setEncoding
        if (MULE_ENCODING_PROPERTY.equals(key))
        {
            setDataType(DataType.builder().type(getDataType().getType()).charset((String) value).build());
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
            setDataType(builder.type(getDataType().getType()).build());
        }
    }

}
