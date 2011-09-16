/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.MessageTransformer;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.PropertyScope;
import org.mule.config.MuleManifest;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.MimeTypes;
import org.mule.transport.NullPayload;
import org.mule.util.ClassUtils;
import org.mule.util.ObjectUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;
import org.mule.util.UUID;
import org.mule.util.store.DeserializationPostInitialisable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultMuleMessage</code> is a wrapper that contains a payload and properties
 * associated with the payload.
 */
public class DefaultMuleMessage implements MuleMessage, ThreadSafeAccess, DeserializationPostInitialisable
{
    protected static final String NOT_SET = "<not set>";

    private static final long serialVersionUID = 1541720810851984844L;
    private static final Log logger = LogFactory.getLog(DefaultMuleMessage.class);
    private static final List<Class<?>> consumableClasses = new ArrayList<Class<?>>();

    /**
     * The default UUID for the message. If the underlying transport has the notion of a
     * message id, this uuid will be ignored
     */
    private String id = UUID.getUUID();

    private transient Object payload;
    private transient Object originalPayload;

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
    @SuppressWarnings("unchecked")
    private Map<String, DataHandler> inboundAttachments = new ConcurrentHashMap();

    /**
     * Collection of attachments that will be sent out with this message
     */
    @SuppressWarnings("unchecked")
    private Map<String, DataHandler> outboundAttachments = new ConcurrentHashMap();

    private transient byte[] cache;
    protected transient MuleContext muleContext;

    // these are transient because serialisation generates a new instance
    // so we allow mutation again (and we can't serialize threads anyway)
    private transient AtomicReference ownerThread = null;
    private transient AtomicBoolean mutable = null;

    private DataType<?> dataType;

    static
    {
        addToConsumableClasses("javax.xml.stream.XMLStreamReader");
        addToConsumableClasses("javax.xml.transform.stream.StreamSource");
        consumableClasses.add(OutputHandler.class);
        consumableClasses.add(InputStream.class);
        consumableClasses.add(Reader.class);
    }

    private static void addToConsumableClasses(String className)
    {
        try
        {
            consumableClasses.add(ClassUtils.loadClass(className, DefaultMuleMessage.class));
        }
        catch (ClassNotFoundException e)
        {
            // ignore
        }
    }

    public DefaultMuleMessage(Object message, MuleContext muleContext)
    {
        this(message, (Map<String, Object>) null, muleContext);
    }

    public DefaultMuleMessage(Object message, Map<String, Object> outboundProperties, MuleContext muleContext)
    {
        this(message, outboundProperties, null, muleContext);
    }

    public DefaultMuleMessage(Object message, Map<String, Object> outboundProperties, Map<String, DataHandler> attachments, MuleContext muleContext)
    {
        this(message, null, outboundProperties, attachments, muleContext);
    }

    public DefaultMuleMessage(Object message, Map<String, Object> inboundProperties,
                              Map<String, Object> outboundProperties, Map<String, DataHandler> attachments,
                              MuleContext muleContext)
    {
        setMuleContext(muleContext);

        if (message instanceof MuleMessage)
        {
            MuleMessage muleMessage = (MuleMessage) message;
            setPayload(muleMessage.getPayload());
            copyMessageProperties(muleMessage);
        }
        else
        {
            setPayload(message);
            originalPayload = message;
        }
        addProperties(inboundProperties, PropertyScope.INBOUND);
        addProperties(outboundProperties);

        //Add inbound attachments
        if (attachments != null)
        {
            inboundAttachments = attachments;
        }

        resetAccessControl();
    }

    public DefaultMuleMessage(Object message, MuleMessage previous, MuleContext muleContext)
    {
        id = previous.getUniqueId();
        setMuleContext(muleContext);
        setEncoding(previous.getEncoding());

        if (message instanceof MuleMessage)
        {
            MuleMessage payloadMessage = (MuleMessage) message;
            setPayload(payloadMessage.getPayload());
            copyMessageProperties(payloadMessage);
        }
        else
        {
            setPayload(message);
            copyMessageProperties(previous);
        }
        originalPayload = previous.getPayload();

        if (previous.getExceptionPayload() != null)
        {
            setExceptionPayload(previous.getExceptionPayload());
        }

        copyAttachments(previous);

        resetAccessControl();
    }

    private void copyMessageProperties(MuleMessage muleMessage)
    {
        // explicitly copy INBOUND message properties over. This cannot be done in the loop below
        Map<String, Object> inboundProperties =
                ((DefaultMuleMessage) muleMessage).properties.getScopedProperties(PropertyScope.INBOUND);
        addInboundProperties(inboundProperties);

        for (PropertyScope scope : PropertyScope.ALL_SCOPES)
        {
            try
            {
                for (String name : muleMessage.getPropertyNames(scope))
                {
                    Object value = muleMessage.getProperty(name, scope);
                    if (value != null)
                    {
                        setProperty(name, value, scope);
                    }
                }
            }
            catch (IllegalArgumentException iae)
            {
                // ignore non-registered property scope
            }
        }
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

    public DefaultMuleMessage(MuleMessage message)
    {
        this(message.getPayload(), message, message.getMuleContext());
    }

    private void setMuleContext(MuleContext context)
    {
        if (context == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("muleContext").getMessage());
        }
        muleContext = context;
    }

    /**
     * {@inheritDoc}
     */
    public <T> T getPayload(Class<T> outputType) throws TransformerException
    {
        return (T) getPayload(DataTypeFactory.create(outputType), getEncoding());
    }


    /**
     * {@inheritDoc}
     */
    public <T> T getPayload(DataType<T> outputType) throws TransformerException
    {
        return getPayload(outputType, getEncoding());
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    /**
     * Will attempt to obtain the payload of this message with the desired Class type. This will
     * try and resolve a transformer that can do this transformation. If a transformer cannot be
     * found an exception is thrown. Any transformers added to the registry will be checked for
     * compatability.
     *
     * @param resultType the desired return type
     * @param encoding   the encoding to use if required
     * @return The converted payload of this message. Note that this method will not alter the
     *         payload of this message <b>unless</b> the payload is an {@link InputStream} in which
     *         case the stream will be read and the payload will become the fully read stream.
     * @throws TransformerException if a transformer cannot be found or there is an error during
     *                              transformation of the payload.
     * @since 3.0
     */
    @SuppressWarnings("unchecked")
    protected <T> T getPayload(DataType<T> resultType, String encoding) throws TransformerException
    {
        // Handle null by ignoring the request
        if (resultType == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("resultType").getMessage());
        }

        DataType source = DataTypeFactory.createFromObject(this);

        // If no conversion is necessary, just return the payload as-is
        if (resultType.isCompatibleWith(source))
        {
            return (T) getPayload();
        }

        // The transformer to execute on this message
        Transformer transformer = muleContext.getRegistry().lookupTransformer(source, resultType);
        if (transformer == null)
        {
            throw new TransformerException(CoreMessages.noTransformerFoundForMessage(source, resultType));
        }

        // Pass in the message itself
        Object result = transformer.transform(this, encoding);

        // Unless we disallow Object.class as a valid return type we need to do this extra check
        if (!resultType.getType().isAssignableFrom(result.getClass()))
        {
            throw new TransformerException(CoreMessages.transformOnObjectNotOfSpecifiedType(resultType, result));
        }

        // If the payload is a stream and we've consumed it, then we should set the payload on the
        // message. This is the only time this method will alter the payload on the message
        if (isPayloadConsumed(source.getType()))
        {
            setPayload(result);
        }

        return (T) result;
    }

    /**
     * Checks if the payload has been consumed for this message. This only applies to Streaming payload types
     * since once the stream has been read, the payload of the message should be updated to represent the data read
     * from the stream
     *
     * @param inputCls the input type of the message payload
     * @return true if the payload message type was stream-based, false otherwise
     */
    protected boolean isPayloadConsumed(Class<?> inputCls)
    {
        return InputStream.class.isAssignableFrom(inputCls) || isConsumedFromAdditional(inputCls);
    }

    private boolean isConsumedFromAdditional(Class<?> inputCls)
    {
        if (consumableClasses.isEmpty())
        {
            return false;
        }

        for (Class<?> c : consumableClasses)
        {
            if (c.isAssignableFrom(inputCls))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Object getOriginalPayload()
    {
        return originalPayload;
    }

    public void setInboundProperty(String key, Object value)
    {
        setProperty(key, value, PropertyScope.INBOUND);
    }

    public void setInvocationProperty(String key, Object value)
    {
        setProperty(key, value, PropertyScope.INVOCATION);
    }

    public void setOutboundProperty(String key, Object value)
    {
        setProperty(key, value, PropertyScope.OUTBOUND);
    }

    public void setSessionProperty(String key, Object value)
    {
        setProperty(key, value, PropertyScope.SESSION);
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty(String key, Object value, PropertyScope scope)
    {
        assertAccess(WRITE);
        if (key != null)
        {
            if (value != null)
            {
                properties.setProperty(key, value, scope);
            }
            else
            {
                logger.warn("setProperty(key, value) called with null value; removing key: " + key
                        + "; please report the following stack trace to " + MuleManifest.getDevListEmail(),
                        new Throwable());
                properties.removeProperty(key);
            }
        }
        else
        {
            logger.warn("setProperty(key, value) ignored because of null key for object: " + value
                    + "; please report the following stack trace to " + MuleManifest.getDevListEmail(),
                    new Throwable());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public Object getProperty(String key)
    {
        assertAccess(READ);
        return properties.getProperty(key, PropertyScope.OUTBOUND);
    }


    /**
     * {@inheritDoc}
     */
    public Object removeProperty(String key)
    {
        //TODO
        //logger.warn("MuleMessage.removeProperty() method is deprecated, use MuleMessage.removeProperty(String, PropertyScope) instead.  This method will be removed in the next point release");
        //return removeProperty(key, PropertyScope.OUTBOUND);
        assertAccess(WRITE);
        return properties.removeProperty(key);
    }

    /**
     * {@inheritDoc}
     */
    public Object removeProperty(String key, PropertyScope scope)
    {
        assertAccess(WRITE);
        return properties.removeProperty(key, scope);
    }

    /**
     * Set a property on the message. This method will now set a value on the outbound scope only.
     *
     * @param key   the key on which to associate the value
     * @param value the property value
     * @see #setInboundProperty(String, Object)
     * @see #setInvocationProperty(String, Object)
     * @see #setOutboundProperty(String, Object)
     * @see #setSessionProperty(String, Object)
     * @deprecated use {@link #setProperty(String, Object, org.mule.api.transport.PropertyScope)} or
     *             preferrably any of the scope-specific set methods.
     */
    @Deprecated
    public void setProperty(String key, Object value)
    {
        assertAccess(WRITE);
        if (key != null)
        {
            if (value != null)
            {
                properties.setProperty(key, value, PropertyScope.OUTBOUND);
            }
            else
            {
                logger.warn("setProperty(key, value) called with null value; removing key: " + key
                        + "; please report the following stack trace to " + MuleManifest.getDevListEmail(),
                        new Throwable());
                properties.removeProperty(key);
            }
        }
        else
        {
            logger.warn("setProperty(key, value) ignored because of null key for object: " + value
                    + "; please report the following stack trace to " + MuleManifest.getDevListEmail(),
                    new Throwable());
        }
    }

    /**
     * {@inheritDoc}
     */
    public final String getPayloadAsString() throws Exception
    {
        assertAccess(READ);
        return getPayloadAsString(getEncoding());
    }

     /**
     * {@inheritDoc}
     */
    public String getPayloadForLogging(String encoding)
    {
        try
        {
            return getPayloadAsString(encoding);
        }
        catch (Exception e)
        {
            return  "[Message could not be converted to string]";
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getPayloadForLogging()
    {
        try
        {
            return getPayloadAsString();
        }
        catch (Exception e)
        {
            return  "[Message could not be converted to string]";
        }
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        assertAccess(READ);
        if (cache != null)
        {
            return cache;
        }
        byte[] result = getPayload(DataType.BYTE_ARRAY_DATA_TYPE);
        if (muleContext.getConfiguration().isCacheMessageAsBytes())
        {
            cache = result;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String getPayloadAsString(String encoding) throws Exception
    {
        assertAccess(READ);
        if (cache != null)
        {
            return new String(cache, encoding);
        }
        String result = getPayload(DataType.STRING_DATA_TYPE, encoding);
        if (muleContext.getConfiguration().isCacheMessageAsBytes())
        {
            cache = result.getBytes(encoding);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated use {@link #getPropertyNames(org.mule.api.transport.PropertyScope)}
     */
    @Deprecated
    public Set<String> getPropertyNames()
    {
        //TODO logger.warn("MuleMessage.getPropertyNames() method is deprecated, use MuleMessage.getOutboundPropertyNames() instead.  This method will be removed in the next point release");
        //return getOutboundPropertyNames();
        assertAccess(READ);
        return properties.getPropertyNames();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getPropertyNames(PropertyScope scope)
    {
        assertAccess(READ);
        if (PropertyScope.SESSION.equals(scope))
        {
            if (RequestContext.getEvent() != null)
            {
                return RequestContext.getEvent().getSession().getPropertyNamesAsSet();
            }
            else
            {
                return Collections.emptySet();
            }
        }
        else
        {
            return properties.getScopedProperties(scope).keySet();
        }
    }

    public Set<String> getInvocationPropertyNames()
    {
        return getPropertyNames(PropertyScope.INVOCATION);
    }

    public Set<String> getInboundPropertyNames()
    {
        return getPropertyNames(PropertyScope.INBOUND);
    }

    public Set<String> getOutboundPropertyNames()
    {
        return getPropertyNames(PropertyScope.OUTBOUND);
    }

    public Set<String> getSessionPropertyNames()
    {
        return getPropertyNames(PropertyScope.SESSION);
    }

    //** {@inheritDoc} */

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public Object getProperty(String name, Object defaultValue)
    {
        //TODO logger.warn("MuleMessage.getProperty() method is deprecated, use MuleMessage.getOutboundProperty() instead.  This method will be removed in the next point release");
        //return getOutboundProperty(name, defaultValue);
        assertAccess(READ);
        return properties.getProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String name, PropertyScope scope)
    {
        assertAccess(READ);
        return (T) properties.getProperty(name, scope);
    }

    public <T> T getInboundProperty(String name, T defaultValue)
    {
        return getProperty(name, PropertyScope.INBOUND, defaultValue);
    }

    public <T> T getInboundProperty(String name)
    {
        return getProperty(name, PropertyScope.INBOUND, (T) null);
    }

    public <T> T getInvocationProperty(String name, T defaultValue)
    {
        return getProperty(name, PropertyScope.INVOCATION, defaultValue);
    }

    public <T> T getInvocationProperty(String name)
    {
        return getInvocationProperty(name, (T) null);
    }

    public <T> T getOutboundProperty(String name, T defaultValue)
    {
        return getProperty(name, PropertyScope.OUTBOUND, defaultValue);
    }

    public <T> T getOutboundProperty(String name)
    {
        return getOutboundProperty(name, (T) null);
    }

    public <T> T getSessionProperty(String name, T defaultValue)
    {
        return getProperty(name, PropertyScope.SESSION, defaultValue);
    }

    public <T> T getSessionProperty(String name)
    {
        return getSessionProperty(name, (T) null);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String name, PropertyScope scope, T defaultValue)
    {
        assertAccess(READ);
        T result;

        //Note that we need to keep the (redundant) casts in here because the compiler compiler complains
        //about primitive types being cast to a generic type
        if (defaultValue instanceof Boolean)
        {
            result = (T) (Boolean) ObjectUtils.getBoolean(getProperty(name, scope), (Boolean) defaultValue);
        }
        else if (defaultValue instanceof Byte)
        {
            result = (T) (Byte) ObjectUtils.getByte(getProperty(name, scope), (Byte) defaultValue);
        }
        else if (defaultValue instanceof Integer)
        {
            result = (T) (Integer) ObjectUtils.getInt(getProperty(name, scope), (Integer) defaultValue);
        }
        else if (defaultValue instanceof Short)
        {
            result = (T) (Short) ObjectUtils.getShort(getProperty(name, scope), (Short) defaultValue);
        }
        else if (defaultValue instanceof Long)
        {
            result = (T) (Long) ObjectUtils.getLong(getProperty(name, scope), (Long) defaultValue);
        }
        else if (defaultValue instanceof Float)
        {
            result = (T) (Float) ObjectUtils.getFloat(getProperty(name, scope), (Float) defaultValue);
        }
        else if (defaultValue instanceof Double)
        {
            result = (T) (Double) ObjectUtils.getDouble(getProperty(name, scope), (Double) defaultValue);
        }
        else if (defaultValue instanceof String)
        {
            result = (T) (String) ObjectUtils.getString(getProperty(name, scope), (String) defaultValue);
        }
        else
        {
            Object temp = getProperty(name, scope);
            if (temp == null)
            {
                return defaultValue;
            }
            else if (defaultValue == null)
            {
                return (T) temp;
            }
            //If defaultValue is set and the result is not null, then validate that they are assignable
            else if (defaultValue.getClass().isAssignableFrom(temp.getClass()))
            {
                result = (T) temp;
            }
            else
            {
                throw new IllegalArgumentException(CoreMessages.objectNotOfCorrectType(temp.getClass(), defaultValue.getClass()).getMessage());
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void setCorrelationId(String id)
    {
        assertAccess(WRITE);
        if (StringUtils.isNotBlank(id))
        {
            setProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY, id, PropertyScope.OUTBOUND);
        }
        else
        {
            removeProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getCorrelationId()
    {
        assertAccess(READ);
        String correlationId = getOutboundProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        if (correlationId == null)
        {
            correlationId = getInboundProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        }

        return correlationId;
    }

    /**
     * {@inheritDoc}
     */
    public void setReplyTo(Object replyTo)
    {
        assertAccess(WRITE);
        if (replyTo != null)
        {
            setProperty(MuleProperties.MULE_REPLY_TO_PROPERTY, replyTo, PropertyScope.OUTBOUND);
        }
        else
        {
            removeProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
            removeProperty(MuleProperties.MULE_REPLY_TO_PROPERTY, PropertyScope.INBOUND);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getReplyTo()
    {
        assertAccess(READ);
        Object replyTo = getProperty(MuleProperties.MULE_REPLY_TO_PROPERTY, PropertyScope.OUTBOUND);
        if (replyTo == null)
        {
            // fallback to inbound, use the requestor's setting if the invocation didn't set any
            replyTo = getProperty(MuleProperties.MULE_REPLY_TO_PROPERTY, PropertyScope.INBOUND);
        }
        return replyTo;
    }

    /**
     * {@inheritDoc}
     */
    public int getCorrelationSequence()
    {
        assertAccess(READ);
        // need to wrap with another getInt() as some transports operate on it as a String
        Object correlationSequence = findPropertyInSpecifiedScopes(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY,
                                                                   PropertyScope.OUTBOUND,
                                                                   PropertyScope.INBOUND);
        return ObjectUtils.getInt(correlationSequence, -1);
    }

    /**
     * {@inheritDoc}
     */
    public void setCorrelationSequence(int sequence)
    {
        assertAccess(WRITE);
        setOutboundProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, sequence);
    }

    /**
     * {@inheritDoc}
     */
    public int getCorrelationGroupSize()
    {
        assertAccess(READ);
        // need to wrap with another getInt() as some transports operate on it as a String
        Object correlationGroupSize = findPropertyInSpecifiedScopes(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY,
                                                                    PropertyScope.OUTBOUND,
                                                                    PropertyScope.INBOUND);
        return ObjectUtils.getInt(correlationGroupSize, -1);
    }

    /**
     * {@inheritDoc}
     */
    public void setCorrelationGroupSize(int size)
    {
        assertAccess(WRITE);
        setOutboundProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, size);
    }

    /**
     * {@inheritDoc}
     */
    public ExceptionPayload getExceptionPayload()
    {
        assertAccess(READ);
        return exceptionPayload;
    }

    /**
     * {@inheritDoc}
     */
    public void setExceptionPayload(ExceptionPayload exceptionPayload)
    {
        assertAccess(WRITE);
        this.exceptionPayload = exceptionPayload;
    }

    @Override
    public String toString()
    {
        assertAccess(READ);
        StringBuffer buf = new StringBuffer(120);
        final String nl = System.getProperty("line.separator");

        // format message for multi-line output, single-line is not readable
        buf.append(nl);
        buf.append(getClass().getName());
        buf.append(nl);
        buf.append("{");
        buf.append(nl);
        buf.append("  id=").append(getUniqueId());
        buf.append(nl);
        buf.append("  payload=").append(getPayload().getClass().getName());
        buf.append(nl);
        buf.append("  correlationId=").append(StringUtils.defaultString(getCorrelationId(), NOT_SET));
        buf.append(nl);
        buf.append("  correlationGroup=").append(getCorrelationGroupSize());
        buf.append(nl);
        buf.append("  correlationSeq=").append(getCorrelationSequence());
        buf.append(nl);
        buf.append("  encoding=").append(getEncoding());
        buf.append(nl);
        buf.append("  exceptionPayload=").append(ObjectUtils.defaultIfNull(exceptionPayload, NOT_SET));
        buf.append(nl);
        buf.append(StringMessageUtils.headersToString(this));
        // no new line here, as headersToString() adds one
        buf.append('}');
        return buf.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public void addAttachment(String name, DataHandler dataHandler) throws Exception
    {
        logger.warn("MuleMessage.addAttachment() method is deprecated, use MuleMessage.addOutboundAttachment() instead.  This method will be removed in the next point release");
        addOutboundAttachment(name, dataHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public void removeAttachment(String name) throws Exception
    {
        logger.warn("MuleMessage.removeAttachment() method is deprecated, use MuleMessage.removeOutboundAttachment() instead.  This method will be removed in the next point release");
        removeOutboundAttachment(name);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public DataHandler getAttachment(String name)
    {
        logger.warn("MuleMessage.getAttachment() method is deprecated, use MuleMessage.getInboundAttachment() instead.  This method will be removed in the next point release");
        return getInboundAttachment(name);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public Set<String> getAttachmentNames()
    {
        logger.warn("MuleMessage.getAttachmentNames() method is deprecated, use MuleMessage.getInboundAttachmentNames() instead.  This method will be removed in the next point release");
        return getInboundAttachmentNames();
    }

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
        else
        {
            dh = new DataHandler(object, contentType);
        }
        outboundAttachments.put(name, dh);
    }

    public void removeOutboundAttachment(String name) throws Exception
    {
        assertAccess(WRITE);
        outboundAttachments.remove(name);
    }

    public DataHandler getInboundAttachment(String name)
    {
        assertAccess(READ);
        return inboundAttachments.get(name);
    }

    public DataHandler getOutboundAttachment(String name)
    {
        assertAccess(READ);
        return outboundAttachments.get(name);
    }

    public Set<String> getInboundAttachmentNames()
    {
        assertAccess(READ);
        return Collections.unmodifiableSet(inboundAttachments.keySet());
    }

    public Set<String> getOutboundAttachmentNames()
    {
        assertAccess(READ);
        return Collections.unmodifiableSet(outboundAttachments.keySet());
    }

    @SuppressWarnings("unchecked")
    public <T> T findPropertyInAnyScope(String name, T defaultValue)
    {
        Object value = findPropertyInSpecifiedScopes(name,
                                                     PropertyScope.OUTBOUND,
                                                     PropertyScope.INVOCATION,
                                                     PropertyScope.SESSION,
                                                     PropertyScope.INBOUND);
        if (value == null)
        {
            return defaultValue;
        }
        return (T) value;
    }

    /**
     * {@inheritDoc}
     */
    public String getEncoding()
    {
        assertAccess(READ);
        String encoding = null;
        if (dataType != null)
        {
            encoding = dataType.getEncoding();
        }
        if (encoding != null)
        {
            return encoding;
        }
        encoding = getOutboundProperty(MuleProperties.MULE_ENCODING_PROPERTY);
        if (encoding != null)
        {
            return encoding;
        }
        else
        {
            return System.getProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setEncoding(String encoding)
    {
        assertAccess(WRITE);
        if (encoding != null)
        {
            setOutboundProperty(MuleProperties.MULE_ENCODING_PROPERTY, encoding);
        }
    }

    /**
     * @param mimeType
     * @since 3.0
     */
    public void setMimeType(String mimeType)
    {
        assertAccess(WRITE);
        if (mimeType != null && !mimeType.equals(MimeTypes.ANY))
        {
            String encoding = getEncoding();
            if (encoding != null)
            {
                mimeType = mimeType + ";charset=" + encoding;
            }
            setOutboundProperty(MuleProperties.CONTENT_TYPE_PROPERTY, mimeType);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addProperties(Map<String, Object> props)
    {
        addProperties(props, properties.getDefaultScope());
    }

    /**
     * {@inheritDoc}
     */
    public void addProperties(Map<String, Object> props, PropertyScope scope)
    {
        assertAccess(WRITE);
        if (props != null)
        {
            synchronized (props)
            {
                for (Map.Entry<String, Object> entry : props.entrySet())
                {
                    setProperty(entry.getKey(), entry.getValue(), scope);
                }
            }
        }
    }

    public void addInboundProperties(Map<String, Object> props)
    {
        properties.addInboundProperties(props);
    }

    /**
     * {@inheritDoc}
     */
    public void clearProperties()
    {
        assertAccess(WRITE);
        //Inbound scope is read-only
        properties.clearProperties(PropertyScope.INVOCATION);
        properties.clearProperties(PropertyScope.OUTBOUND);
    }

    /**
     * {@inheritDoc}
     */
    public void clearProperties(PropertyScope scope)
    {
        assertAccess(WRITE);
        properties.clearProperties(scope);
    }


    /**
     * {@inheritDoc}
     */
    public Object getPayload()
    {
        return payload;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setPayload(Object payload)
    {
        if (payload == null)
        {
            this.payload = NullPayload.getInstance();
        }
        else
        {
            this.payload = payload;
        }
        cache = null;
    }

    /**
     * {@inheritDoc}
     */
    public void release()
    {
        cache = null;
    }

    /**
     * {@inheritDoc}
     */
    public void applyTransformers(MuleEvent event, List<? extends Transformer> transformers) throws MuleException
    {
        applyTransformers(event, transformers, null);
    }

    /**
     * {@inheritDoc}
     */
    public void applyTransformers(MuleEvent event, Transformer... transformers) throws MuleException
    {
        applyTransformers(event, Arrays.asList(transformers), null);
    }

    public void applyTransformers(MuleEvent event, List<? extends Transformer> transformers, Class<?> outputType) throws MuleException
    {
        if (!transformers.isEmpty())
        {
            applyAllTransformers(event, transformers);
        }

        if (null != outputType && !getPayload().getClass().isAssignableFrom(outputType))
        {
            setPayload(getPayload(DataTypeFactory.create(outputType)));
        }
    }

    protected void applyAllTransformers(MuleEvent event, List<? extends Transformer> transformers) throws MuleException
    {
        if (!transformers.isEmpty())
        {
            for (Transformer transformer : transformers)
            {
                if (getPayload() == null)
                {
                    if (transformer.isAcceptNull())
                    {
                        setPayload(NullPayload.getInstance());
                        setDataType(null);
                    }
                    else
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Transformer " + transformer +
                                    " doesn't support the null payload, exiting from transformer chain.");
                        }
                        break;
                    }
                }

                Class<?> srcCls = getPayload().getClass();
                if (transformer.isSourceDataTypeSupported(DataTypeFactory.create(srcCls)))
                {
                    Object result;
                    if (transformer instanceof MessageTransformer)
                    {
                        result = ((MessageTransformer) transformer).transform(this, event);
                    }
                    else
                    {
                        result = transformer.transform(this);
                    }
                    // Update the RequestContext with the result of the transformation.
                    RequestContext.internalRewriteEvent(this, false);

                    if (originalPayload == null && muleContext.getConfiguration().isCacheMessageOriginalPayload())
                    {
                        originalPayload = payload;
                    }

                    if (result instanceof MuleMessage)
                    {
                        if (!result.equals(this))
                        {
                            // Only copy the payload and properties of mule message
                            // transformer result if the message is a different
                            // instance
                            synchronized (this)
                            {
                                MuleMessage resultMessage = (MuleMessage) result;
                                setPayload(resultMessage.getPayload());
                                originalPayload = resultMessage.getOriginalPayload();
                                copyMessageProperties(resultMessage);
                                copyAttachments(resultMessage);
                            }
                        }
                    }
                    else
                    {
                        setPayload(result);
                    }
                    setDataType(transformer.getReturnDataType());
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Transformer " + transformer + " doesn't support the source payload: " + srcCls);
                    }
                    if (!transformer.isIgnoreBadInput())
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Exiting from transformer chain (ignoreBadInput = false)");
                        }
                        break;
                    }
                }
            }
        }
    }

    protected void setDataType(DataType<?> dt)
    {
        dataType = dt;
        setEncoding(dt == null ? null : dt.getEncoding());
        setMimeType(dt == null ? null : dt.getMimeType());
    }

    //////////////////////////////// ThreadSafeAccess Impl ///////////////////////////////

    /**
     * {@inheritDoc}
     */
    public ThreadSafeAccess newThreadCopy()
    {
        return new DefaultMuleMessage(this);
    }

    /**
     * {@inheritDoc}
     */
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
            ownerThread = new AtomicReference();
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

    protected boolean isDisabled()
    {
        return !AccessControl.isFailOnMessageScribbling();
    }

    protected IllegalStateException newException(String message)
    {
        IllegalStateException exception = new IllegalStateException(message);
        logger.warn("Message access violation", exception);
        return exception;
    }

    /**
     * Determines if the payload of this message is consumable i.e. it can't be read
     * more than once. This is here temporarily without adding to MuleMessage
     * interface until MULE-4256 is implemented.
     */
    public boolean isConsumable()
    {
        return isConsumedFromAdditional(this.getPayload().getClass());
    }

    private void writeObject(ObjectOutputStream out) throws Exception
    {
        out.defaultWriteObject();

        if (payload instanceof Serializable)
        {
            out.writeBoolean(true);
            out.writeObject(payload);
        }
        else
        {
            out.writeBoolean(false);
            byte[] serializablePayload = getPayloadAsBytes();
            out.writeInt(serializablePayload.length);
            out.write(serializablePayload);
        }

        // TODO: we don't serialize the originalPayload for now
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        boolean payloadWasSerialized = in.readBoolean();
        if (payloadWasSerialized)
        {
            payload = in.readObject();
        }
        else
        {
            int payloadSize = in.readInt();
            byte[] serializedPayload = new byte[payloadSize];
            in.read(serializedPayload);
            payload = serializedPayload;
        }
    }

    /**
     * Invoked after deserialization. This is called when the marker interface
     * {@link org.mule.util.store.DeserializationPostInitialisable} is used. This will get invoked
     * after the object has been deserialized passing in the current mulecontext when using either
     * {@link org.mule.transformer.wire.SerializationWireFormat},
     * {@link org.mule.transformer.wire.SerializedMuleMessageWireFormat} or the
     * {@link org.mule.transformer.simple.ByteArrayToSerializable} transformer.
     *
     * @param muleContext the current muleContext instance
     * @throws MuleException if there is an error initializing
     */
    public void initAfterDeserialisation(MuleContext muleContext) throws MuleException
    {
        this.muleContext = muleContext;
    }

    public DataType<?> getDataType()
    {
        return dataType;
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public int getIntProperty(String name, int defaultValue)
    {
        assertAccess(READ);
        logger.warn("MuleMessage.getIntProperty() method is deprecated, use MuleMessage.getInboundProperty() instead.  This method will be removed in the next point release");
        return getInboundProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public long getLongProperty(String name, long defaultValue)
    {
        assertAccess(READ);
        logger.warn("MuleMessage.getLongProperty() method is deprecated, use MuleMessage.getInboundProperty() instead.  This method will be removed in the next point release");
        return getInboundProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public double getDoubleProperty(String name, double defaultValue)
    {
        assertAccess(READ);
        logger.warn("MuleMessage.getDoubleProperty() method is deprecated, use MuleMessage.getInboundProperty() instead.  This method will be removed in the next point release");
        return getInboundProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        assertAccess(READ);
        logger.warn("MuleMessage.getBooleanProperty() method is deprecated, use MuleMessage.getInboundProperty() instead.  This method will be removed in the next point release");
        return getInboundProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public void setBooleanProperty(String name, boolean value)
    {
        assertAccess(WRITE);
        logger.warn("MuleMessage.setBooleanProperty() method is deprecated, use MuleMessage.setOutboundProperty() instead.  This method will be removed in the next point release");
        setOutboundProperty(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public void setIntProperty(String name, int value)
    {
        assertAccess(WRITE);
        logger.warn("MuleMessage.setIntProperty() method is deprecated, use MuleMessage.setOutboundProperty() instead.  This method will be removed in the next point release");
        setOutboundProperty(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public void setLongProperty(String name, long value)
    {
        assertAccess(WRITE);
        logger.warn("MuleMessage.setLongProperty() method is deprecated, use MuleMessage.setOutboundProperty() instead.  This method will be removed in the next point release");
        setOutboundProperty(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public void setDoubleProperty(String name, double value)
    {
        assertAccess(WRITE);
        logger.warn("MuleMessage.setDoubleProperty() method is deprecated, use MuleMessage.setOutboundProperty() instead.  This method will be removed in the next point release");
        setOutboundProperty(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public String getStringProperty(String name, String defaultValue)
    {
        assertAccess(READ);
        logger.warn("MuleMessage.getStringProperty() method is deprecated, use MuleMessage.getInboundProperty() instead.  This method will be removed in the next point release");
        return getInboundProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public void setStringProperty(String name, String value)
    {
        assertAccess(WRITE);
        logger.warn("MuleMessage.setStringProperty() method is deprecated, use MuleMessage.setOutboundProperty() instead.  This method will be removed in the next point release");
        setOutboundProperty(name, value);
    }

    /**
     * Find property in one of the specified scopes, in order
     */
    @SuppressWarnings("unchecked")
    public <T> T findPropertyInSpecifiedScopes(String name, PropertyScope... scopesToSearch)
    {
        for (PropertyScope scope : scopesToSearch)
        {
            Object result = getProperty(name, scope);
            if (result != null)
            {
                return (T) result;
            }
        }
        return null;
    }

    public MuleMessage createInboundMessage() throws Exception
    {
        DefaultMuleMessage newMessage =  new DefaultMuleMessage(getPayload(), this, getMuleContext());
        copyToInbound(newMessage);
        return newMessage;
    }

    /**
     * copy outbound artifacts to inbound artifacts in the new message
     */
    protected void copyToInbound(DefaultMuleMessage newMessage) throws Exception
    {
        // Copy message, but put all outbound properties and attachments on inbound scope.
        // We ignore inbound and invocation scopes since the VM receiver needs to behave the
        // same way as any other receiver in Mule and would only receive inbound headers
        // and attachments
        Map<String, DataHandler> attachments = new HashMap<String, DataHandler>(3);
        for (String name : getOutboundAttachmentNames())
        {
            attachments.put(name, getOutboundAttachment(name));
        }

        Map<String, Object> newInboundProperties = new HashMap<String, Object>(3);
        for (String name : getOutboundPropertyNames())
        {
            newInboundProperties.put(name, getOutboundProperty(name));
        }

        newMessage.clearProperties(PropertyScope.INBOUND);
        newMessage.clearProperties(PropertyScope.INVOCATION);
        newMessage.clearProperties(PropertyScope.OUTBOUND);

        for (String s : newInboundProperties.keySet())
        {
            newMessage.setInboundProperty(s, newInboundProperties.get(s));
        }

        newMessage.inboundAttachments.clear();
        newMessage.outboundAttachments.clear();

        for (String s : attachments.keySet())
        {
            newMessage.addInboundAttachment(s, attachments.get(s));
        }

        newMessage.setCorrelationId(getCorrelationId());
        newMessage.setCorrelationGroupSize(getCorrelationGroupSize());
        newMessage.setCorrelationSequence(getCorrelationSequence());
        newMessage.setReplyTo(getReplyTo());
        newMessage.setEncoding(getEncoding());
    }
}
