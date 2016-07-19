/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;
import static org.mule.runtime.core.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.core.util.ObjectUtils.getBoolean;
import static org.mule.runtime.core.util.ObjectUtils.getByte;
import static org.mule.runtime.core.util.ObjectUtils.getDouble;
import static org.mule.runtime.core.util.ObjectUtils.getFloat;
import static org.mule.runtime.core.util.ObjectUtils.getInt;
import static org.mule.runtime.core.util.ObjectUtils.getLong;
import static org.mule.runtime.core.util.ObjectUtils.getShort;
import static org.mule.runtime.core.util.ObjectUtils.getString;

import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.PropertyScope;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.ExceptionPayload;
import org.mule.runtime.core.api.MessageProperties;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.api.MuleMessage.CollectionBuilder;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.metadata.DefaultCollectionDataType;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.util.CaseInsensitiveMapWrapper;
import org.mule.runtime.core.util.CopyOnWriteCaseInsensitiveMap;
import org.mule.runtime.core.util.MapUtils;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.runtime.core.util.StringMessageUtils;
import org.mule.runtime.core.util.UUID;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
    private Attributes attributes = NULL_ATTRIBUTES;

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
        message.getCorrelation().getId().ifPresent(v -> this.correlationId = v);
        message.getCorrelation().getSequence().ifPresent(v -> this.correlationSequence = v);
        message.getCorrelation().getGroupSize().ifPresent(v -> this.correlationGroupSize = v);
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
        return this;
    }

    @Override
    public Builder addInboundProperty(String key, Serializable value, MediaType mediaType)
    {
        inboundProperties.put(key, new TypedValue(value, DataType.builder().type(value.getClass()).mediaType(mediaType).build()));
        return this;
    }

    @Override
    public Builder addInboundProperty(String key, Serializable value, DataType dataType)
    {
        inboundProperties.put(key, new TypedValue(value, dataType));
        return this;
    }

    @Override
    public Builder addOutboundProperty(String key, Serializable value)
    {
        outboundProperties.put(key, new TypedValue(value, value != null ? DataType.fromObject(value) : DataType.OBJECT));
        return this;
    }

    @Override
    public Builder addOutboundProperty(String key, Serializable value, MediaType mediaType)
    {
        outboundProperties.put(key, new TypedValue(value, DataType.builder().type(value.getClass()).mediaType(mediaType).build()));
        return this;
    }

    @Override
    public Builder addOutboundProperty(String key, Serializable value, DataType dataType)
    {
        outboundProperties.put(key, new TypedValue(value, dataType));
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
        return new MuleMessageImplementation(id != null ? id : UUID.getUUID(), rootId,
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

    /**
     * <code>DefaultMuleMessage</code> is a wrapper that contains a payload and properties
     * associated with the payload.
     */
    public static class MuleMessageImplementation implements MuleMessage, DeserializationPostInitialisable
    {
        private static final String NOT_SET = "<not set>";

        private static final long serialVersionUID = 1541720810851984845L;
        private static final Logger logger = LoggerFactory.getLogger(MuleMessageImplementation.class);

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

        private Correlation correlation;

        // these are transient because serialisation generates a new instance
        // so we allow mutation again (and we can't serialize threads anyway)
        private transient AtomicReference<Thread> ownerThread = null;
        private transient AtomicBoolean mutable = null;

        private transient TypedValue typedValue;
        private Attributes attributes;

        private Serializable replyTo;

        private MuleMessageImplementation(String id, String rootId, TypedValue typedValue, Attributes attributes,
                                  Map<String, TypedValue<Serializable>> inboundProperties,
                                  Map<String, TypedValue<Serializable>> outboundProperties,
                                  Map<String, DataHandler> inboundAttachments, Map<String, DataHandler> outboundAttachments,
                                  String correlationId, Integer correlationGroupSize, Integer correlationSequence,
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
            this.correlation = new Correlation(correlationId, correlationGroupSize, correlationSequence);
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

        @Override
        public Correlation getCorrelation()
        {
            return correlation;
        }

        private void setReplyTo(Object replyTo)
        {
            if (replyTo != null)
            {
                if(!(replyTo instanceof Serializable))
                {
                    logger.warn("ReplyTo " + replyTo + " is not serializable and will not be propagated by Mule");
                }
                else
                {
                    this.replyTo = (Serializable) replyTo;
                }
            }
            else
            {
                this.replyTo = null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getReplyTo()
        {
            return replyTo;
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
            buf.append("  correlation=").append(getCorrelation().toString());
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

        @Override
        public Attributes getAttributes()
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
            if (!(obj instanceof MuleMessageImplementation))
            {
                return false;
            }
            return this.id.equals(((MuleMessageImplementation)obj).id);
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
        }

        public void setOutboundProperty(String key, Serializable value)
        {
            properties.setOutboundProperty(key, value);
        }

        public void setOutboundProperty(String key, Serializable value, DataType dataType)
        {
            properties.setOutboundProperty(key, value, dataType);
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

        /**
         * This object maintains case-sensitive inbound and outbound scoped messages properties.
         * <ol>
         * <li> {@link PropertyScope#INBOUND} Contains properties that were on the message when
         * it was received by Mule. This scope is read-only.</li>
         * <li>{@link PropertyScope#OUTBOUND} Any properties set in this scope will be attached
         * to any outbound messages resulting from this message. This is the default scope.</li>
         * </ol>
         */
        public static class MessagePropertiesContext implements MessageProperties, Serializable
        {
            private static final long serialVersionUID = -5230693402768953742L;
            private static final Logger logger = LoggerFactory.getLogger(MessagePropertiesContext.class);


            protected CopyOnWriteCaseInsensitiveMap<String, TypedValue<? extends Serializable>> inboundMap;
            protected CopyOnWriteCaseInsensitiveMap<String, TypedValue<? extends Serializable>> outboundMap;

            public MessagePropertiesContext()
            {
                inboundMap = new CopyOnWriteCaseInsensitiveMap<>();
                outboundMap = new CopyOnWriteCaseInsensitiveMap<>();
            }

            public MessagePropertiesContext(MessagePropertiesContext previous)
            {
                inboundMap = previous.inboundMap.clone();
                outboundMap = previous.outboundMap.clone();
            }

            @Override
            public <T extends Serializable> T getInboundProperty(String name)
            {
                return getInboundProperty(name, null);
            }

            @Override
            public <T extends Serializable> T getInboundProperty(String name, T defaultValue)
            {
                return getValueOrDefault((TypedValue<T>) inboundMap.get(name), defaultValue);
            }

            @Override
            public <T extends Serializable> T getOutboundProperty(String name)
            {
                return getOutboundProperty(name, null);
            }

            @Override
            public <T extends Serializable> T getOutboundProperty(String name, T defaultValue)
            {
                return getValueOrDefault((TypedValue<T>) outboundMap.get(name), defaultValue);
            }

            public <T extends Serializable> void setInboundProperty(String key, T value, DataType dataType)
            {
                if (key != null)
                {
                    if (value == null || value instanceof NullPayload)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("setProperty(key, value) called with null value; removing key: " + key);
                        }
                        removeInboundProperty(key);
                    }
                    else
                    {
                        inboundMap.put(key, new TypedValue(value, dataType));
                    }
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("setProperty(key, value) invoked with null key. Ignoring this entry");
                    }
                }
            }

            public void setOutboundProperty(String key, Serializable value)
            {
                setOutboundProperty(key, value, DataType.fromObject(value));
            }

            public <T extends Serializable> void setOutboundProperty(String key, T value, DataType dataType)
            {
                if (key != null)
                {
                    if (value == null || value instanceof NullPayload)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("setProperty(key, value) called with null value; removing key: " + key);
                        }
                        removeOutboundProperty(key);
                    }
                    else
                    {
                        outboundMap.put(key, new TypedValue(value, dataType));
                    }
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("setProperty(key, value) invoked with null key. Ignoring this entry");
                    }
                }
            }

            public <T extends Serializable> T removeInboundProperty(String key)
            {
                TypedValue value = inboundMap.remove(key);
                return value == null ? null : (T) value.getValue();
            }

            public <T extends Serializable> T removeOutboundProperty(String key)
            {
                TypedValue value = outboundMap.remove(key);
                return value == null ? null : (T) value.getValue();
            }

            @Override
            public DataType getInboundPropertyDataType(String name)
            {
                TypedValue typedValue = inboundMap.get(name);
                return typedValue == null ? null : typedValue.getDataType();
            }

            @Override
            public DataType getOutboundPropertyDataType(String name)
            {
                TypedValue typedValue = outboundMap.get(name);
                return typedValue == null ? null : typedValue.getDataType();
            }

            @Override
            public Set<String> getInboundPropertyNames()
            {
                return inboundMap.keySet();
            }

            @Override
            public Set<String> getOutboundPropertyNames()
            {
                return outboundMap.keySet();
            }

            @Override
            public String toString()
            {
                StringBuilder buf = new StringBuilder(128);
                buf.append("Properties{");
                buf.append(PropertyScope.INBOUND_NAME).append(":");
                buf.append(MapUtils.toString(inboundMap, false));
                buf.append(", ");
                buf.append(PropertyScope.OUTBOUND_NAME).append(":");
                buf.append(MapUtils.toString(inboundMap, false));
                buf.append("}");
                return buf.toString();
            }

            private <T extends Serializable> T getValueOrDefault(TypedValue<T> typedValue, T defaultValue)
            {
                if (typedValue == null)
                {
                    return defaultValue;
                }
                T value = typedValue.getValue();
                //Note that we need to keep the (redundant) casts in here because the compiler compiler complains
                //about primitive types being cast to a generic type
                if (defaultValue == null)
                {
                    return value;
                }
                else if (defaultValue instanceof Boolean)
                {
                    return  (T) (Boolean) getBoolean(value, (Boolean) defaultValue);
                }
                else if (defaultValue instanceof Byte)
                {
                    return (T) (Byte) getByte(value, (Byte) defaultValue);
                }
                else if (defaultValue instanceof Integer)
                {
                    return (T) (Integer) getInt(value, (Integer) defaultValue);
                }
                else if (defaultValue instanceof Short)
                {
                    return (T) (Short) getShort(value, (Short) defaultValue);
                }
                else if (defaultValue instanceof Long)
                {
                    return (T) (Long) getLong(value, (Long) defaultValue);
                }
                else if (defaultValue instanceof Float)
                {
                    return (T) (Float) getFloat(value, (Float) defaultValue);
                }
                else if (defaultValue instanceof Double)
                {
                    return (T) (Double) getDouble(value, (Double) defaultValue);
                }
                else if (defaultValue instanceof String)
                {
                    return (T) getString(value, (String) defaultValue);
                }
                else
                {
                    if (value == null)
                    {
                        return defaultValue;
                    }
                    //If defaultValue is set and the result is not null, then validate that they are assignable
                    else if (defaultValue.getClass().isAssignableFrom(value.getClass()))
                    {
                        return value;
                    }
                    else
                    {
                        throw new IllegalArgumentException(CoreMessages.objectNotOfCorrectType(value.getClass(), defaultValue.getClass()).getMessage());
                    }
                }
            }

        }
    }
}
