/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;
import static org.mule.runtime.core.api.config.MuleProperties.CONTENT_TYPE_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_ID_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ENCODING_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;

import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.core.api.ExceptionPayload;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.MutableMuleMessage;
import org.mule.runtime.core.api.ThreadSafeAccess;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.message.ds.ByteArrayDataSource;
import org.mule.runtime.core.message.ds.InputStreamDataSource;
import org.mule.runtime.core.message.ds.StringDataSource;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.runtime.core.util.StringMessageUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.UUID;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>DefaultMuleMessage</code> is a wrapper that contains a payload and properties
 * associated with the payload.
 */
public class DefaultMuleMessage extends TypedValue<Object> implements MutableMuleMessage, ThreadSafeAccess, DeserializationPostInitialisable
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
    private transient Map<String, DataHandler> inboundAttachments = new HashMap<String, DataHandler>();

    /**
     * Collection of attachments that will be sent out with this message
     */
    private transient Map<String, DataHandler> outboundAttachments = new HashMap<String, DataHandler>();

    private transient MuleContext muleContext;

    // these are transient because serialisation generates a new instance
    // so we allow mutation again (and we can't serialize threads anyway)
    private transient AtomicReference<Thread> ownerThread = null;
    private transient AtomicBoolean mutable = null;

    private Serializable attributes;

    private static DataType<?> getMessageDataType(MuleMessage previous, Object payload)
    {
        if (payload instanceof MuleMessage)
        {
            return ((MuleMessage) payload).getDataType();
        }
        else
        {
            // TODO MULE-9855: Make MuleMessage immutable
            DataType<?> dataType = DataType.builder(previous.getDataType()).type(payload.getClass()).build();

            return dataType;
        }
    }

    private static DataType<?> getCloningMessageDataType(MuleMessage previous)
    {
        // TODO MULE-9855: Make MuleMessage immutable
        return previous.getDataType();
    }

    /**
     * Creates a new message instance with the given value.  The data-type will be generated based on the Java
     * type of the value provided.
     *
     * @param value  the value (or payload) of the message being created.
     * @param <T> the type of the value
     */
    public <T> DefaultMuleMessage(T value)
    {
        this(value, (DataType<T>) null, null, null);
    }

    /**
     * @deprecated this is a temporal workaround because the message requires the mule context. As the message
     * refactor progresses, {@link DefaultMuleMessage(T, DataType)} should be use instead
     */
    @Deprecated
    public <T> DefaultMuleMessage(T value, MuleContext muleContext)
    {
        this(value, (DataType<T>) null, null, muleContext);
    }

    /**
     * Creates a new message instance with the given value and data type.
     *
     * @param value  the value (or payload) of the message being created.
     * @param dataType the data type the describes the value.
     * @param <T> the type of the value
     */
    public <T> DefaultMuleMessage(T value, DataType<T> dataType)
    {
        this(value, dataType, null, null);
    }

    /**
     * Creates a new message instance with the given value and attributes object
     *
     * @param value  the value (or payload) of the message being created.
     * @param attributes a connector specific object that contains additional attributes related to the message value or
     *                   it's source.
     * @param <T> the type of the value
     */
    public <T> DefaultMuleMessage(T value, Serializable attributes)
    {
        this(value, (DataType<T>) null, attributes, null);
    }

    /**
     * @deprecated this is a temporal workaround because the message requires the mule context. As the message
     * refactor progresses, {@link DefaultMuleMessage(T, DataType)} should be use instead
     */
    @Deprecated
    public <T> DefaultMuleMessage(T value, DataType<T> dataType, MuleContext muleContext)
    {
        this(value, dataType, null, muleContext);
    }

    /**
     * Creates a new message instance with the given value, data type and attributes object
     *
     * @param value  the value (or payload) of the message being created.
     * @param dataType the data type the describes the value.
     * @param attributes a connector specific object that contains additional attributes related to the message value or
     *                   it's source.
     * @param <T> the type of the value
     */
    public <T> DefaultMuleMessage(T value, DataType<T> dataType, Serializable attributes)
    {
        this(value, dataType, attributes, null);
    }

    /**
     * @deprecated this is a temporal workaround because the message requires the mule context. As the message
     * refactor progresses, {@link DefaultMuleMessage(T, DataType, Serializable)} should be use instead
     */
    @Deprecated
    public <T> DefaultMuleMessage(T value, DataType<T> dataType, Serializable attributes, MuleContext muleContext)
    {
        super(resolveValue(value), resolveDataType(value, dataType));
        id = UUID.getUUID();
        rootId = id;
        this.attributes = attributes;
        setMuleContext(muleContext);

        if (value instanceof MuleMessage)
        {
            copyMessageProperties((MuleMessage) value);
        }
        resetAccessControl();
    }

    public DefaultMuleMessage(MuleMessage message)
    {
        this(message.getPayload(), message, message.getMuleContext(), getCloningMessageDataType(message));
    }

    public DefaultMuleMessage(Object message, Map<String, Serializable> outboundProperties, MuleContext muleContext)
    {
        this(message, outboundProperties, null, muleContext);
    }

    public DefaultMuleMessage(Object message, Map<String, Serializable> outboundProperties, Map<String, DataHandler> attachments, MuleContext muleContext)
    {
        this(message, null, outboundProperties, attachments, muleContext);
    }

    public DefaultMuleMessage(Object message, Map<String, Serializable> inboundProperties,
                              Map<String, Serializable> outboundProperties, Map<String, DataHandler> attachments,
                              MuleContext muleContext)
    {
        this(message, inboundProperties, outboundProperties, attachments, muleContext, createDefaultDataType(message));
    }

    public DefaultMuleMessage(Object message, Map<String, Serializable> inboundProperties,
                              Map<String, Serializable> outboundProperties, Map<String, DataHandler> attachments,
                              MuleContext muleContext, DataType dataType)
    {
        super(resolveValue(message), dataType);
        id =  UUID.getUUID();
        rootId = id;

        setMuleContext(muleContext);

        if (message instanceof MuleMessage)
        {
            copyMessageProperties((MuleMessage) message);
        }
        addInboundProperties(inboundProperties);
        addOutboundProperties(outboundProperties);

        //Add inbound attachments
        if (attachments != null)
        {
            inboundAttachments = attachments;
        }

        resetAccessControl();
    }

    private static Object resolveValue(Object value)
    {
        if (value instanceof MuleMessage)
        {
            value = ((MuleMessage) value).getPayload();
        }
        return value != null ? value : NullPayload.getInstance();
    }

    private static DataType resolveDataType(Object value, DataType dataType)
    {
        return dataType != null ? dataType : createDefaultDataType(value);
    }

    public DefaultMuleMessage(Object message, MuleMessage previous, MuleContext muleContext)
    {
        this(message, previous, muleContext, getMessageDataType(previous, message));
    }

    public DefaultMuleMessage(Object message, MuleMessage previous, MuleContext muleContext, DataType<?> dataType)
    {
        super(resolveValue(message), (DataType<Object>) dataType);
        id = previous.getUniqueId();
        rootId = previous.getMessageRootId();
        setMuleContext(muleContext);

        if(message instanceof MuleMessage)
        {
            copyMessageProperties((MuleMessage) message);
        }
        else
        {
            copyMessageProperties(previous);
        }

        if (getDataType().getEncoding() == null)
        {
            setEncoding(previous.getEncoding());
        }

        if (previous.getExceptionPayload() != null)
        {
            setExceptionPayload(previous.getExceptionPayload());
        }

        copyAttachments(previous);
        attributes = previous.getAttributes();

        resetAccessControl();
    }

    private void copyAttachments(MuleMessage previous)
    {
        if (previous.getInboundAttachmentNames().size() > 0)
        {
            for (String name : previous.getInboundAttachmentNames())
            {
                try
                {
                    inboundAttachments.put(name, previous.getInboundAttachment(name));
                }
                catch (Exception e)
                {
                    throw new MuleRuntimeException(CoreMessages.failedToReadAttachment(name), e);
                }
            }
        }

        if (previous.getOutboundAttachmentNames().size() > 0)
        {
            for (String name : previous.getOutboundAttachmentNames())
            {
                try
                {
                    addOutboundAttachment(name, previous.getOutboundAttachment(name));
                }
                catch (Exception e)
                {
                    throw new MuleRuntimeException(CoreMessages.failedToReadAttachment(name), e);
                }
            }
        }
    }

    private static DataType<?> createDefaultDataType(Object payload)
    {
        Class<?> type = payload == null ? Object.class : payload.getClass();
        return DataType.builder().type(type).build();
    }

    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MuleContext getMuleContext()
    {
        return muleContext;
    }


    /**
     * Checks if the payload has been consumed for this message. This only applies to Streaming payload types
     * since once the stream has been read, the payload of the message should be updated to represent the data read
     * from the stream
     *
     * @param inputCls the input type of the message payload
     * @return true if the payload message type was stream-based, false otherwise
     */
    boolean isPayloadConsumed(Class<?> inputCls)
    {
        return InputStream.class.isAssignableFrom(inputCls) || ClassUtils.isConsumable(inputCls);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUniqueId()
    {
        assertAccess(READ);
        return id;
    }

    public void setUniqueId(String uid)
    {
        assertAccess(WRITE);
        id = uid;
    }

    @Override
    public String getMessageRootId()
    {
        assertAccess(READ);
        return rootId;
    }

    @Override
    public void setMessageRootId(String rid)
    {
        assertAccess(WRITE);
        rootId = rid;
    }

    @Override
    public void propagateRootId(MuleMessage parent)
    {
        assertAccess(WRITE);
        if (parent != null)
        {
            rootId = parent.getMessageRootId();
        }
    }

    @Override
    public void setCorrelationId(String id)
    {
        assertAccess(WRITE);
        if (StringUtils.isNotBlank(id))
        {
            setOutboundProperty(MULE_CORRELATION_ID_PROPERTY, id, null);
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
        assertAccess(READ);
        String correlationId = getOutboundProperty(MULE_CORRELATION_ID_PROPERTY);
        if (correlationId == null)
        {
            correlationId = getInboundProperty(MULE_CORRELATION_ID_PROPERTY);
        }

        return correlationId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReplyTo(Object replyTo)
    {
        assertAccess(WRITE);
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
        assertAccess(READ);
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
    public int getCorrelationSequence()
    {
        assertAccess(READ);
        // need to wrap with another getInt() as some transports operate on it as a String
        Object correlationSequence = findProperty(MULE_CORRELATION_SEQUENCE_PROPERTY);
        return ObjectUtils.getInt(correlationSequence, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCorrelationSequence(int sequence)
    {
        assertAccess(WRITE);
        setOutboundProperty(MULE_CORRELATION_SEQUENCE_PROPERTY, sequence);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCorrelationGroupSize()
    {
        assertAccess(READ);
        // need to wrap with another getInt() as some transports operate on it as a String
        Object correlationGroupSize = findProperty(MULE_CORRELATION_GROUP_SIZE_PROPERTY);
        return ObjectUtils.getInt(correlationGroupSize, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCorrelationGroupSize(int size)
    {
        assertAccess(WRITE);
        setOutboundProperty(MULE_CORRELATION_GROUP_SIZE_PROPERTY, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExceptionPayload getExceptionPayload()
    {
        assertAccess(READ);
        return exceptionPayload;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExceptionPayload(ExceptionPayload exceptionPayload)
    {
        assertAccess(WRITE);
        this.exceptionPayload = exceptionPayload;
    }

    @Override
    public String toString()
    {
        assertAccess(READ);
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
        buf.append("  encoding=").append(getEncoding());
        buf.append(LINE_SEPARATOR);
        buf.append("  exceptionPayload=").append(ObjectUtils.defaultIfNull(exceptionPayload, NOT_SET));
        buf.append(LINE_SEPARATOR);
        buf.append(StringMessageUtils.headersToString(this));
        // no new line here, as headersToString() adds one
        buf.append('}');
        return buf.toString();
    }

    @Override
    public void addOutboundAttachment(String name, DataHandler dataHandler) throws Exception
    {
        assertAccess(WRITE);
        outboundAttachments.put(name, dataHandler);
    }

    ///TODO this should not be here, but needed so that a message factory can add attachments
    //This is not part of the API

    public void addInboundAttachment(String name, DataHandler dataHandler) throws Exception
    {
        assertAccess(WRITE);
        inboundAttachments.put(name, dataHandler);
    }

    @Override
    public void addOutboundAttachment(String name, Object object, String contentType) throws Exception
    {
        assertAccess(WRITE);
        DataHandler dh;
        if (object instanceof File)
        {
            if (contentType != null)
            {
                dh = new DataHandler(new FileInputStream((File) object), contentType);

            }
            else
            {
                dh = new DataHandler(new FileDataSource((File) object));
            }
        }
        else if (object instanceof URL)
        {
            if (contentType != null)
            {
                dh = new DataHandler(((URL) object).openStream(), contentType);
            }
            else
            {
                dh = new DataHandler((URL) object);
            }
        }
        else if (object instanceof String)
        {
            if (contentType != null)
            {
                dh = new DataHandler(new StringDataSource((String) object, name, contentType));
            }
            else
            {
                dh = new DataHandler(new StringDataSource((String) object, name));
            }
        }
        else if (object instanceof byte[] && contentType != null)
        {
            dh = new DataHandler(new ByteArrayDataSource((byte[]) object, contentType, name));
        }
        else if (object instanceof InputStream && contentType != null)
        {
            dh = new DataHandler(new InputStreamDataSource((InputStream) object, contentType, name));
        }
        else
        {
            dh = new DataHandler(object, contentType);
        }
        outboundAttachments.put(name, dh);
    }

    @Override
    public void removeOutboundAttachment(String name) throws Exception
    {
        assertAccess(WRITE);
        outboundAttachments.remove(name);
    }

    @Override
    public DataHandler getInboundAttachment(String name)
    {
        assertAccess(READ);
        return inboundAttachments.get(name);
    }

    @Override
    public DataHandler getOutboundAttachment(String name)
    {
        assertAccess(READ);
        return outboundAttachments.get(name);
    }

    @Override
    public Set<String> getInboundAttachmentNames()
    {
        assertAccess(READ);
        return unmodifiableSet(inboundAttachments.keySet());
    }

    @Override
    public Set<String> getOutboundAttachmentNames()
    {
        assertAccess(READ);
        return unmodifiableSet(outboundAttachments.keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEncoding()
    {
        assertAccess(READ);
        String encoding = null;
        if (getDataType() != null)
        {
            encoding = getDataType().getEncoding();
            if (encoding != null)
            {
                return encoding;
            }
        }
        encoding = getOutboundProperty(MULE_ENCODING_PROPERTY);
        if (encoding != null)
        {
            return encoding;
        }
        else
        {
            return getDefaultEncoding(muleContext);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEncoding(String encoding)
    {
        assertAccess(WRITE);
        setDataType(DataType.builder(getDataType()).encoding(encoding).build());
    }

    /**
     * @param mimeType
     * @since 3.0
     */
    public void setMimeType(String mimeType)
    {
        assertAccess(WRITE);
        setDataType(DataType.builder(getDataType()).mimeType(mimeType).build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAttachments()
    {
        assertAccess(WRITE);
        outboundAttachments.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getPayload()
    {
        return getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setPayload(Object payload)
    {
        DataType<?> newDataType;
        if (payload == null || payload instanceof NullPayload)
        {
            newDataType = DataType.OBJECT;
        }
        else
        {
            newDataType = DataType.forType(payload.getClass());
        }

        setPayload(payload, newDataType);
    }

    @Override
    public void setPayload(Object payload, DataType<?> dataType)
    {
        if (payload == null)
        {
            setValue(NullPayload.getInstance());
        }
        else
        {
            setValue(payload);
        }

        setDataType(dataType);
    }

    public void setAttributes(Serializable attributes)
    {
        this.attributes = attributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release()
    {
    }

    @Override
    protected void setDataType(DataType dt)
    {
        assertAccess(WRITE);
        super.setDataType(dt);
    }

    //////////////////////////////// ThreadSafeAccess Impl ///////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public ThreadSafeAccess newThreadCopy()
    {
        return new DefaultMuleMessage(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetAccessControl()
    {
        // just reset the internal state here as this method is explicitly intended not to
        // be used from the outside
        if (ownerThread != null)
        {
            ownerThread.set(null);
        }
        if (mutable != null)
        {
            mutable.set(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void assertAccess(boolean write)
    {
        if (AccessControl.isAssertMessageAccess())
        {
            initAccessControl();
            setOwner();
            checkMutable(write);
        }
    }

    private synchronized void initAccessControl()
    {
        if (null == ownerThread)
        {
            ownerThread = new AtomicReference<Thread>();
        }
        if (null == mutable)
        {
            mutable = new AtomicBoolean(true);
        }
    }

    private void setOwner()
    {
        if (null == ownerThread.get())
        {
            ownerThread.compareAndSet(null, Thread.currentThread());
        }
    }

    private void checkMutable(boolean write)
    {

        // IF YOU SEE AN EXCEPTION THAT IS RAISED FROM WITHIN THIS CODE
        // ============================================================
        //
        // First, understand that the exception here is not the "real" problem.  These exceptions
        // give early warning of a much more serious issue that results in unreliable and unpredictable
        // code - more than one thread is attempting to change the contents of a message.
        //
        // Having said that, you can disable these exceptions by defining
        // MuleProperties.MULE_THREAD_UNSAFE_MESSAGES_PROPERTY (mule.disable.threadsafemessages)
        // (i.e., by adding -Dmule.disable.threadsafemessages=true to the java command line).
        //
        // To remove the underlying cause, however, you probably need to do one of:
        //
        // - make sure that the message you are using correctly implements the ThreadSafeAccess
        //   interface
        //
        // - make sure that dispatcher and receiver classes copy ThreadSafeAccess instances when
        //   they are passed between threads

        Thread currentThread = Thread.currentThread();
        if (currentThread.equals(ownerThread.get()))
        {
            if (write && !mutable.get())
            {
                if (isDisabled())
                {
                    logger.warn("Writing to immutable message (exception disabled)");
                }
                else
                {
                    throw newException("Cannot write to immutable message");
                }
            }
        }
        else
        {
            if (write)
            {
                if (isDisabled())
                {
                    logger.warn("Non-owner writing to message (exception disabled)");
                }
                else
                {
                    throw newException("Only owner thread can write to message: "
                            + ownerThread.get() + "/" + Thread.currentThread());
                }
            }
        }
    }

    private boolean isDisabled()
    {
        return !AccessControl.isFailOnMessageScribbling();
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
            toWrite = new HashMap<String, SerializedDataHandler>(attachments.size());
            for (Map.Entry<String, DataHandler> entry : attachments.entrySet())
            {
                String name = entry.getKey();
                toWrite.put(name, new SerializedDataHandler(name, entry.getValue(), muleContext));
            }
        }

        return toWrite;
    }

    @Override
    protected void serializeValue(ObjectOutputStream out) throws Exception
    {
        if (getValue() instanceof Serializable)
        {
            out.writeBoolean(true);
            out.writeObject(getValue());
        }
        else
        {
            out.writeBoolean(false);
            byte[] valueAsByteArray = (byte[]) muleContext.getTransformationService().transform(this, DataType.BYTE_ARRAY).getPayload();
            out.writeInt(valueAsByteArray.length);
            new DataOutputStream(out).write(valueAsByteArray);
        }
    }

    @Override
    protected void deserializeValue(ObjectInputStream in) throws Exception
    {
        boolean valueSerialized = in.readBoolean();
        if (valueSerialized)
        {
            setValue(in.readObject());
        }
        else
        {
            int length = in.readInt();
            byte[] valueAsByteArray = new byte[length];
            new DataInputStream(in).readFully(valueAsByteArray);
            setValue(valueAsByteArray);
        }
    }

    private Map<String, DataHandler> deserializeAttachments(Map<String, SerializedDataHandler> attachments) throws IOException
    {
        Map<String, DataHandler> toReturn;
        if (attachments == null)
        {
            toReturn = null;
        }
        else
        {
            toReturn = new HashMap<String, DataHandler>(attachments.size());
            for (Map.Entry<String, SerializedDataHandler> entry : attachments.entrySet())
            {
                toReturn.put(entry.getKey(), entry.getValue().getHandler());
            }
        }

        return toReturn;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        inboundAttachments = deserializeAttachments((Map<String, SerializedDataHandler>)in.readObject());
        outboundAttachments = deserializeAttachments((Map<String, SerializedDataHandler>)in.readObject());
    }

    /**
     * Invoked after deserialization. This is called when the marker interface
     * {@link org.mule.runtime.core.util.store.DeserializationPostInitialisable} is used. This will get invoked
     * after the object has been deserialized passing in the current mulecontext when using either
     * {@link org.mule.runtime.core.transformer.wire.SerializationWireFormat},
     * {@link org.mule.runtime.core.transformer.wire.SerializedMuleMessageWireFormat} or the
     * {@link org.mule.runtime.core.transformer.simple.ByteArrayToSerializable} transformer.
     *
     * @param context the current muleContext instance
     * @throws MuleException if there is an error initializing
     */
    public void initAfterDeserialisation(MuleContext context) throws MuleException
    {
        this.muleContext = context;
        if (this.inboundAttachments == null)
        {
            this.inboundAttachments = new HashMap<String, DataHandler>();
        }

        if (this.outboundAttachments == null)
        {
            this.outboundAttachments = new HashMap<String, DataHandler>();
        }
    }

    @Override
    public <Payload, Attributes extends Serializable> org.mule.runtime.api.message.MuleMessage<Payload, Attributes> asNewMessage()
    {
        return (org.mule.runtime.api.message.MuleMessage<Payload, Attributes>) this;
    }

    /**
     * Find property by searching outbound and then inbound scopes in order.
     * @param name name of the property to find
     * @return value of the property or null if property is not found in either scope
     */
    @SuppressWarnings("unchecked")
    private <T> T findProperty(String name)
    {
        T result = getOutboundProperty(name);
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
    public MuleMessage createInboundMessage() throws Exception
    {
        Object payload = getPayload();

        if (payload instanceof List && ((List) payload).stream().filter(item -> item instanceof DefaultMuleMessage)
                                               .count() > 0)
        {
            List<Object> newListPayload = new ArrayList<>();
            for (Object item : (List) payload)
            {
                if (item instanceof DefaultMuleMessage)
                {
                    newListPayload.add(copyToInbound((DefaultMuleMessage) item, ((MuleMessage) item)
                            .getPayload()));
                }
                else
                {
                    newListPayload.add(item);
                }
            }
            payload = newListPayload;
        }
        return copyToInbound(this, payload);
    }

    /**
     * copy outbound artifacts to inbound artifacts in the new message
     */
    private MuleMessage copyToInbound(DefaultMuleMessage currentMessage, Object payload) throws Exception
    {
        DefaultMuleMessage newMessage = new DefaultMuleMessage(payload, currentMessage, currentMessage.getMuleContext());

        // Copy message, but put all outbound properties and attachments on inbound scope.
        // We ignore inbound and invocation scopes since the VM receiver needs to behave the
        // same way as any other receiver in Mule and would only receive inbound headers
        // and attachments
        Map<String, DataHandler> attachments = new HashMap<String, DataHandler>(3);
        for (String name : currentMessage.getOutboundAttachmentNames())
        {
            attachments.put(name, getOutboundAttachment(name));
        }

        Map<String, Serializable> newInboundProperties = new HashMap<>(3);
        for (String name : currentMessage.getOutboundPropertyNames())
        {
            newInboundProperties.put(name, currentMessage.getOutboundProperty(name));
        }

        newMessage.properties.clearInboundProperties();
        newMessage.properties.clearOutboundProperties();

        for (Map.Entry<String, Serializable> s : newInboundProperties.entrySet())
        {
            DataType<? extends Serializable> propertyDataType = currentMessage.getOutboundPropertyDataType(s.getKey());

            newMessage.setInboundProperty(s.getKey(), s.getValue(), (DataType<Serializable>) propertyDataType);
        }

        newMessage.inboundAttachments.clear();
        newMessage.outboundAttachments.clear();

        for (Map.Entry<String, DataHandler> s : attachments.entrySet())
        {
            newMessage.addInboundAttachment(s.getKey(), s.getValue());
        }

        newMessage.setCorrelationId(currentMessage.getCorrelationId());
        newMessage.setCorrelationGroupSize(currentMessage.getCorrelationGroupSize());
        newMessage.setCorrelationSequence(currentMessage.getCorrelationSequence());
        newMessage.setReplyTo(currentMessage.getReplyTo());
        newMessage.setEncoding(currentMessage.getEncoding());
        return newMessage;
    }

    @Override
    public Serializable getAttributes()
    {
        return attributes;
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
    public <T extends Serializable> T getInboundProperty(String name)
    {
        return properties.getInboundProperty(name);
    }

    @Override
    public <T extends Serializable> T getInboundProperty(String name, T defaultValue)
    {
        return properties.getInboundProperty(name, defaultValue);
    }

    @Override
    public <T extends Serializable> T getOutboundProperty(String name)
    {
        return properties.getOutboundProperty(name);
    }

    @Override
    public <T extends Serializable> T getOutboundProperty(String name, T defaultValue)
    {
        return properties.getOutboundProperty(name, defaultValue);
    }

    @Override
    public void setInboundProperty(String key, Serializable value)
    {
        properties.setInboundProperty(key, value);
        updateDataTypeWithProperty(key, value);
    }

    @Override
    public <T extends Serializable> void setInboundProperty(String key, T value, DataType<T> dataType)
    {
        properties.setInboundProperty(key, value, dataType);
        updateDataTypeWithProperty(key, value);
    }

    @Override
    public void addInboundProperties(Map<String, Serializable> props)
    {
        assertAccess(WRITE);
        properties.addInboundProperties(props);
        if (props != null)
        {
            props.entrySet().stream().forEach(entry -> updateDataTypeWithProperty(entry.getKey(), entry.getValue()));
        }
    }

    @Override
    public void setOutboundProperty(String key, Serializable value)
    {
        properties.setOutboundProperty(key, value);
        updateDataTypeWithProperty(key, value);
    }

    @Override
    public <T extends Serializable> void setOutboundProperty(String key, T value, DataType<T> dataType)
    {
        properties.setOutboundProperty(key, value, dataType);
        updateDataTypeWithProperty(key, value);
    }

    @Override
    public void addOutboundProperties(Map<String, Serializable> props)
    {
        assertAccess(WRITE);
        properties.addOutboundProperties(props);
        if (props != null)
        {
            props.entrySet().stream().forEach(entry -> updateDataTypeWithProperty(entry.getKey(), entry.getValue()));
        }
    }

    @Override
    public Serializable removeInboundProperty(String key)
    {
        return properties.removeInboundProperty(key);
    }

    @Override
    public Serializable removeOutboundProperty(String key)
    {
        return properties.removeOutboundProperty(key);
    }

    @Override
    public void clearInboundProperties()
    {
        properties.clearInboundProperties();
    }

    @Override
    public void clearOutboundProperties()
    {
        properties.clearOutboundProperties();
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
    public void copyProperty(String key)
    {
        properties.copyProperty(key);
    }

    @Override
    public DataType<? extends Serializable> getInboundPropertyDataType(String name)
    {
        return properties.getInboundPropertyDataType(name);
    }

    @Override
    public DataType<? extends Serializable> getOutboundPropertyDataType(String name)
    {
        return properties.getOutboundPropertyDataType(name);
    }

    private void copyMessageProperties(MuleMessage muleMessage)
    {
        if (muleMessage instanceof DefaultMuleMessage)
        {
            properties = new MessagePropertiesContext(((DefaultMuleMessage) muleMessage).properties);
        }
        else
        {
            for (String name : muleMessage.getInboundPropertyNames())
            {
                Serializable value = muleMessage.getInboundProperty(name);
                if (value != null)
                {
                    properties.setInboundProperty(name, value, muleMessage.getInboundPropertyDataType(name));
                }
            }
            for (String name : muleMessage.getOutboundPropertyNames())
            {
                Serializable value = muleMessage.getOutboundProperty(name);
                if (value != null)
                {
                    properties.setOutboundProperty(name, value, muleMessage.getOutboundPropertyDataType(name));
                }
            }
        }
    }

    private void updateDataTypeWithProperty(String key, Object value)
    {
        final DataTypeBuilder builder = DataType.builder(getDataType());

        // updates dataType when encoding is updated using a property instead of using #setEncoding
        if (MULE_ENCODING_PROPERTY.equals(key))
        {
            builder.encoding((String) value);
        }
        else if (CONTENT_TYPE_PROPERTY.equalsIgnoreCase(key))
        {
            try
            {
                builder.mimeType((String) value);
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
                    builder.encoding(encoding);
                }
            }
        }

        setDataType(builder.build());
    }

    /**
     * TODO MULE-9856 Replace with the builder
     * <p>
     * This method here is needed for calls from mocks. Mockito doesn't support default methods in
     * interfaces.
     */
    @Override
    @Deprecated
    public MuleMessage transform(Function<MutableMuleMessage, MuleMessage> transform)
    {
        MutableMuleMessage copy = new DefaultMuleMessage(this);
        return transform.apply(copy);
    }
}
