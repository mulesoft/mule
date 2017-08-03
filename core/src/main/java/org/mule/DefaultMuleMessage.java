/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.lang.String.format;
import static org.mule.api.config.MuleProperties.CONTENT_TYPE_PROPERTY;
import static org.mule.api.config.MuleProperties.MULE_ENCODING_PROPERTY;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.transformer.types.SimpleDataType.CHARSET_PARAM;
import static org.mule.util.SystemUtils.LINE_SEPARATOR;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.MessageTransformer;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.message.ds.ByteArrayDataSource;
import org.mule.message.ds.InputStreamDataSource;
import org.mule.message.ds.StringDataSource;
import org.mule.transformer.TransformerUtils;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.MimeTypes;
import org.mule.transformer.types.TypedValue;
import org.mule.transport.NullPayload;
import org.mule.util.ClassUtils;
import org.mule.util.ObjectUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;
import org.mule.util.SystemUtils;
import org.mule.util.UUID;
import org.mule.util.store.DeserializationPostInitialisable;

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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultMuleMessage</code> is a wrapper that contains a payload and properties
 * associated with the payload.
 */
public class DefaultMuleMessage implements MuleMessage, ThreadSafeAccess, DeserializationPostInitialisable
{
    protected static final String NOT_SET = "<not set>";

    private static final long serialVersionUID = 1541720810851984845L;
    private static final Log logger = LogFactory.getLog(DefaultMuleMessage.class);

    /**
     * The default UUID for the message. If the underlying transport has the notion of a
     * message id, this uuid will be ignored
     */
    private String id;
    private String rootId;

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
    private transient Map<String, DataHandler> inboundAttachments = new HashMap<String, DataHandler>();

    /**
     * Collection of attachments that will be sent out with this message
     */
    private transient Map<String, DataHandler> outboundAttachments = new HashMap<String, DataHandler>();

    private transient byte[] cache;
    protected transient MuleContext muleContext;

    // these are transient because serialisation generates a new instance
    // so we allow mutation again (and we can't serialize threads anyway)
    private transient AtomicReference<Thread> ownerThread = null;
    private transient AtomicBoolean mutable = null;

    private DataType<?> dataType;

    private static DataType<?> getMessageDataType(MuleMessage previous, Object payload)
    {
        if (payload instanceof MuleMessage)
        {
            return ((MuleMessage) payload).getDataType();
        }
        else
        {
            DataType<?> dataType = DataTypeFactory.create(payload.getClass(), previous.getDataType().getMimeType());
            dataType.setEncoding(previous.getDataType().getEncoding());

            return dataType;
        }
    }

    private static DataType<?> getCloningMessageDataType(MuleMessage previous)
    {
        DataType<?> dataType = DataTypeFactory.create(previous.getDataType().getType(), previous.getDataType().getMimeType());
        dataType.setEncoding(previous.getDataType().getEncoding());

        return dataType;
    }

    public DefaultMuleMessage(MuleMessage message)
    {
        this(message.getPayload(), message, message.getMuleContext(), getCloningMessageDataType(message));
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
        this(message, inboundProperties, outboundProperties, attachments, muleContext, createDefaultDataType(message, muleContext));
    }

    public DefaultMuleMessage(Object message, Map<String, Object> inboundProperties,
                              Map<String, Object> outboundProperties, Map<String, DataHandler> attachments,
                              MuleContext muleContext, DataType dataType)
    {
        id =  UUID.getUUID();
        rootId = id;

        setMuleContext(muleContext);

        if (message instanceof MuleMessage)
        {
            MuleMessage muleMessage = (MuleMessage) message;
            setPayload(muleMessage.getPayload(), dataType);
            copyMessageProperties(muleMessage);
        }
        else
        {
            setPayload(message, dataType);
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
        this(message, previous, muleContext, getMessageDataType(previous, message));
    }

    private DefaultMuleMessage(Object message, MuleMessage previous, MuleContext muleContext, DataType<?> dataType)
    {
        id = previous.getUniqueId();
        rootId = previous.getMessageRootId();
        setMuleContext(muleContext);

        DataType newDataType  = dataType.cloneDataType();

        if (message instanceof MuleMessage)
        {
            MuleMessage payloadMessage = (MuleMessage) message;
            setPayload(payloadMessage.getPayload(), newDataType);
            copyMessageProperties(payloadMessage);
        }
        else
        {
            setPayload(message, newDataType);
            copyMessagePropertiesContext(previous);
        }

        originalPayload = previous.getPayload();
        setEncoding(previous.getEncoding());

        if (previous.getExceptionPayload() != null)
        {
            setExceptionPayload(previous.getExceptionPayload());
        }

        if (previous instanceof DefaultMuleMessage)
        {
            setInvocationProperties(((DefaultMuleMessage) previous).properties.invocationMap);
            setSessionProperties(((DefaultMuleMessage) previous).properties.sessionMap);
        }

        copyAttachments(previous);

        resetAccessControl();
    }

    private void copyMessagePropertiesContext(MuleMessage muleMessage)
    {
        if (muleMessage instanceof DefaultMuleMessage)
        {
            properties = new MessagePropertiesContext(((DefaultMuleMessage) muleMessage).properties);
        }
        else
        {
            copyMessageProperties(muleMessage);
        }
    }

    protected void copyMessageProperties(MuleMessage muleMessage)
    {
        for (PropertyScope scope : new PropertyScope[]{PropertyScope.INBOUND, PropertyScope.OUTBOUND})
        {
            try
            {
                for (String name : muleMessage.getPropertyNames(scope))
                {
                    Object value = muleMessage.getProperty(name, scope);
                    if (value != null)
                    {
                        setPropertyInternal(name, value, scope, DataTypeFactory.createFromObject(value));
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

    private static DataType<?> createDefaultDataType(Object payload, MuleContext muleContext)
    {
        DataType<?> dataType = DataTypeFactory.create(payload == null ? Object.class : payload.getClass());
        dataType.setEncoding(SystemUtils.getDefaultEncoding(muleContext));

        return dataType;
    }

    public void setMuleContext(MuleContext context)
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
    @Override
    public <T> T getPayload(Class<T> outputType) throws TransformerException
    {
        return getPayload(DataTypeFactory.create(outputType), getEncoding());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getPayload(DataType<T> outputType) throws TransformerException
    {
        return getPayload(outputType, getEncoding());
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
            setPayload(result, dataType);
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
        return InputStream.class.isAssignableFrom(inputCls) || ClassUtils.isConsumable(inputCls);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getOriginalPayload()
    {
        return originalPayload;
    }

    public void setInboundProperty(String key, Object value)
    {
        setProperty(key, value, PropertyScope.INBOUND);
    }

    public void setInboundProperty(String key, Object value, DataType<?> dataType)
    {
        setProperty(key, value, PropertyScope.INBOUND, dataType);
    }

    @Override
    public void setInvocationProperty(String key, Object value)
    {
        setProperty(key, value, PropertyScope.INVOCATION);
    }

    @Override
    public void setInvocationProperty(String key, Object value, DataType<?> dataType)
    {
        setProperty(key, value, PropertyScope.INVOCATION, dataType);
    }

    @Override
    public void setOutboundProperty(String key, Object value)
    {
        setProperty(key, value, PropertyScope.OUTBOUND, DataTypeFactory.createFromObject(value));
    }

    @Override
    public void setOutboundProperty(String key, Object value, DataType<?> dataType)
    {
       setProperty(key, value, PropertyScope.OUTBOUND, dataType);
    }

    @Override
    public void setSessionProperty(String key, Object value)
    {
        setProperty(key, value, PropertyScope.SESSION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProperty(String key, Object value, PropertyScope scope)
    {
        DataType dataType = DataTypeFactory.createFromObject(value);
        setProperty(key, value, scope, dataType);
    }

    @Override
    public void setProperty(String key, Object value, PropertyScope scope, DataType<?> dataType)
    {
        setPropertyInternal(key, value, scope, dataType);

        updateDataTypeWithProperty(key, value);
    }

    private void setPropertyInternal(String key, Object value, PropertyScope scope, DataType<?> dataType)
    {
        assertAccess(WRITE);
        if (key != null)
        {
            if (value == null || value instanceof NullPayload)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("setProperty(key, value) called with null value; removing key: " + key);
                }
                properties.removeProperty(key);
            }
            else
            {
                properties.setProperty(key, value, scope, dataType);
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

    private void updateDataTypeWithProperty(String key, Object value)
    {
        // updates dataType when encoding is updated using a property instead of using #setEncoding
        if (MULE_ENCODING_PROPERTY.equals(key))
        {
            dataType.setEncoding(getStringPropertyValue(MULE_ENCODING_PROPERTY, value));
        }
        else if (CONTENT_TYPE_PROPERTY.equalsIgnoreCase(key))
        {
            try
            {
                final String contentType = getStringPropertyValue(CONTENT_TYPE_PROPERTY, value);
                MimeType mimeType = new MimeType(contentType);

                dataType.setMimeType(mimeType.getPrimaryType() + "/" + mimeType.getSubType());

                String encoding = mimeType.getParameter(CHARSET_PARAM);
                if (!StringUtils.isEmpty(encoding))
                {
                    dataType.setEncoding(encoding);
                }
            }
            catch (MimeTypeParseException e)
            {
                if (Boolean.parseBoolean(System.getProperty(SYSTEM_PROPERTY_PREFIX + "strictContentType")))
                {
                    throw new IllegalArgumentException("Invalid Content-Type property value", e);
                }
                else
                {
                    String encoding = Charset.defaultCharset().name();
                    logger.warn(format("%s when parsing Content-Type '%s': %s", e.getClass().getName(), value, e.getMessage()));
                    logger.warn(format("Using defualt encoding: %s", encoding));
                    dataType.setEncoding(encoding);
                }
            }
        }
    }

    /*
       There are properties, like encoding and content-type, that are used to update the message datatype.
       Those properties are supposed to be Strings, and in general, they are.
       But, there are cases, like HTTP requests, where content-type can be present more than once, so the
       request will contain a list containing all the content-type values.
       This method manages this situation by checking the type of the property value and attempts to get a
       unique String value from it. In case of having multiple String values, the first one will be used.
       If the value is not a String or collection of Strings, an error will be thrown.
     */
    private String getStringPropertyValue(String propertyName, Object value)
    {
        String propertyValue;

        if (value instanceof String)
        {
           propertyValue = (String) value;
        }
        else if (value instanceof Collection)
        {
            if (((Collection) value).isEmpty())
            {
                throw new IllegalArgumentException(format("Unsupported value for '%s' property. Expected 'java.lang.String' but was an empty collection", propertyName));
            }
            final Object collectionItem = ((Collection) value).iterator().next();
            if (collectionItem instanceof String)
            {
                propertyValue = (String) collectionItem;
            }
            else if (collectionItem == null)
            {
                propertyValue = null;
            }
            else
            {
                throw new IllegalArgumentException(format("Unsupported type for '%s' property. Expected 'java.lang.String' but was '%s'", propertyName, value.getClass().getName()));
            }
        }
        else if (value == null)
        {
           propertyValue = null;
        }
        else
        {
            throw new IllegalArgumentException(format("Unsupported type for '%s' property. Expected 'java.lang.String' but was '%s'", propertyName, value.getClass().getName()));
        }

        return propertyValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public Object getProperty(String key)
    {
        assertAccess(READ);
        return properties.getProperty(key, PropertyScope.OUTBOUND);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object removeProperty(String key)
    {
        assertAccess(WRITE);
        return properties.removeProperty(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
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
                logger.warn("setProperty(key, value) called with null value; removing key: " + key,
                        new Throwable());
                properties.removeProperty(key);
            }

            updateDataTypeWithProperty(key, value);
        }
        else
        {
            logger.warn("setProperty(key, value) ignored because of null key for object: " + value,
                    new Throwable());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getPayloadAsString() throws Exception
    {
        assertAccess(READ);
        return getPayloadAsString(getEncoding());
    }

     /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
    @Deprecated
    public Set<String> getPropertyNames()
    {
        assertAccess(READ);
        return properties.getPropertyNames(PropertyScope.OUTBOUND);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getPropertyNames(PropertyScope scope)
    {
        assertAccess(READ);
        return properties.getScopedProperties(scope).keySet();
    }

    @Override
    public Set<String> getInvocationPropertyNames()
    {
        return getPropertyNames(PropertyScope.INVOCATION);
    }

    @Override
    public Set<String> getInboundPropertyNames()
    {
        return getPropertyNames(PropertyScope.INBOUND);
    }

    @Override
    public Set<String> getOutboundPropertyNames()
    {
        return getPropertyNames(PropertyScope.OUTBOUND);
    }

    @Override
    public Set<String> getSessionPropertyNames()
    {
        return getPropertyNames(PropertyScope.SESSION);
    }

    //** {@inheritDoc} */

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

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getProperty(String name, Object defaultValue)
    {
        assertAccess(READ);
        return properties.getProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String name, PropertyScope scope)
    {
        assertAccess(READ);
        return (T) properties.getProperty(name, scope);
    }

    @Override
    public <T> T getInboundProperty(String name, T defaultValue)
    {
        return getProperty(name, PropertyScope.INBOUND, defaultValue);
    }

    @Override
    public <T> T getInboundProperty(String name)
    {
        return getProperty(name, PropertyScope.INBOUND, (T) null);
    }

    @Override
    public <T> T getInvocationProperty(String name, T defaultValue)
    {
        return getProperty(name, PropertyScope.INVOCATION, defaultValue);
    }

    @Override
    public <T> T getInvocationProperty(String name)
    {
        return getInvocationProperty(name, (T) null);
    }

    @Override
    public <T> T getOutboundProperty(String name, T defaultValue)
    {
        return getProperty(name, PropertyScope.OUTBOUND, defaultValue);
    }

    @Override
    public <T> T getOutboundProperty(String name)
    {
        return getOutboundProperty(name, (T) null);
    }

    @Override
    public <T> T getSessionProperty(String name, T defaultValue)
    {
        return getProperty(name, PropertyScope.SESSION, defaultValue);
    }

    @Override
    public <T> T getSessionProperty(String name)
    {
        return getSessionProperty(name, (T) null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
            result = (T) ObjectUtils.getString(getProperty(name, scope), (String) defaultValue);
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
    public void setCorrelationSequence(int sequence)
    {
        assertAccess(WRITE);
        setOutboundProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, sequence);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public void setCorrelationGroupSize(int size)
    {
        assertAccess(WRITE);
        setOutboundProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, size);
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public void addAttachment(String name, DataHandler dataHandler) throws Exception
    {
        logger.warn("MuleMessage.addAttachment() method is deprecated, use MuleMessage.addOutboundAttachment() instead.  This method will be removed in the next point release");
        addOutboundAttachment(name, dataHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public void removeAttachment(String name) throws Exception
    {
        logger.warn("MuleMessage.removeAttachment() method is deprecated, use MuleMessage.removeOutboundAttachment() instead.  This method will be removed in the next point release");
        removeOutboundAttachment(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public DataHandler getAttachment(String name)
    {
        logger.warn("MuleMessage.getAttachment() method is deprecated, use MuleMessage.getInboundAttachment() instead.  This method will be removed in the next point release");
        return getInboundAttachment(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public Set<String> getAttachmentNames()
    {
        logger.warn("MuleMessage.getAttachmentNames() method is deprecated, use MuleMessage.getInboundAttachmentNames() instead.  This method will be removed in the next point release");
        return getInboundAttachmentNames();
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
        return Collections.unmodifiableSet(inboundAttachments.keySet());
    }

    @Override
    public Set<String> getOutboundAttachmentNames()
    {
        assertAccess(READ);
        return Collections.unmodifiableSet(outboundAttachments.keySet());
    }

    @Override
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
    @Override
    public String getEncoding()
    {
        assertAccess(READ);
        String encoding = null;
        if (dataType != null)
        {
            encoding = dataType.getEncoding();
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
            return SystemUtils.getDefaultEncoding(muleContext);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEncoding(String encoding)
    {
        assertAccess(WRITE);
        dataType.setEncoding(encoding);
    }

    /**
     * @param mimeType
     * @since 3.0
     */
    public void setMimeType(String mimeType)
    {
        assertAccess(WRITE);
        dataType.setMimeType(mimeType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addProperties(Map<String, Object> props)
    {
        addProperties(props, properties.getDefaultScope());
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
        addProperties(props, PropertyScope.INBOUND);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public void clearProperties(PropertyScope scope)
    {
        assertAccess(WRITE);
        properties.clearProperties(scope);
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
        return payload;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setPayload(Object payload)
    {
        DataType  newDataType;
        if (payload == null || payload instanceof NullPayload)
        {
            newDataType = DataTypeFactory.create(Object.class, null);
        }
        else
        {
            newDataType = DataTypeFactory.create(payload.getClass(), null);
        }

        setPayload(payload, newDataType);
    }

    @Override
    public void setPayload(Object payload, DataType<?> dataType)
    {
        if (payload == null)
        {
            this.payload = NullPayload.getInstance();
        }
        else
        {
            this.payload = payload;
        }

        this.dataType = dataType.cloneDataType();

        cache = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release()
    {
        cache = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyTransformers(MuleEvent event, List<? extends Transformer> transformers) throws MuleException
    {
        applyTransformers(event, transformers, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyTransformers(MuleEvent event, Transformer... transformers) throws MuleException
    {
        applyTransformers(event, Arrays.asList(transformers), null);
    }

    @Override
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
            for (int index = 0; index < transformers.size(); index++)
            {
                Transformer transformer = transformers.get(index);

                Class<?> srcCls = getPayload().getClass();
                DataType<?> originalSourceType = DataTypeFactory.create(srcCls);

                if (transformer.isSourceDataTypeSupported(originalSourceType))
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Using " + transformer + " to transform payload.");
                    }
                    transformMessage(event, transformer);
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Transformer " + transformer + " doesn't support the source payload: " + srcCls);
                    }

                    if (useExtendedTransformations())
                    {
                        if (canSkipTransformer(transformers, index))
                        {
                            continue;
                        }

                        // Resolves implicit conversion if possible
                        Transformer implicitTransformer = muleContext.getDataTypeConverterResolver().resolve(originalSourceType, transformer.getSourceDataTypes());

                        if (implicitTransformer != null)
                        {
                            if (logger.isDebugEnabled())
                            {
                                logger.debug("Performing implicit transformation with: " + transformer);
                            }
                            transformMessage(event, implicitTransformer);
                            transformMessage(event, transformer);
                        }
                        else
                        {
                            throw new IllegalArgumentException("Cannot apply transformer " + transformer + " on source payload: " + srcCls);
                        }
                    }
                    else if (!transformer.isIgnoreBadInput())
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

    private boolean canSkipTransformer(List<? extends Transformer> transformers, int index)
    {
        Transformer transformer = transformers.get(index);

        boolean skipConverter = false;

        if (transformer instanceof Converter)
        {
            if (index == transformers.size() - 1)
            {
                try
                {
                    TransformerUtils.checkTransformerReturnClass(transformer, payload);
                    skipConverter = true;
                }
                catch (TransformerException e)
                {
                    // Converter cannot be skipped
                }
            }
            else
            {
                skipConverter= true;
            }
        }

        if (skipConverter)
        {
            logger.debug("Skipping converter: " + transformer);
        }

        return skipConverter;
    }

    private boolean useExtendedTransformations()
    {
        boolean result = true;
        if (muleContext != null && muleContext.getConfiguration() != null)
        {
            result = muleContext.getConfiguration().useExtendedTransformations();
        }

        return result;
    }

    private void transformMessage(MuleEvent event, Transformer transformer) throws TransformerMessagingException, TransformerException
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
                    setPayload(resultMessage.getPayload(), resultMessage.getDataType());
                    originalPayload = resultMessage.getOriginalPayload();
                    copyMessageProperties(resultMessage);
                    copyAttachments(resultMessage);
                }
            }
        }
        else
        {
            final DataType<?> mergedDataType = mergeDataType(dataType, transformer.getReturnDataType(), result != null ? result.getClass() : null);
            setPayload(result, mergedDataType);
        }
    }

    private DataType<?> mergeDataType(DataType<?> original, DataType<?> transformed, Class<?> payloadTransformedClass)
    {
        String mimeType = transformed.getMimeType() == null || MimeTypes.ANY.equals(transformed.getMimeType()) ? original.getMimeType() : transformed.getMimeType();
        String encoding = transformed.getEncoding() == null ? this.getEncoding() : transformed.getEncoding();
        // In case if the transformed dataType is an Object type we could keep the original type if it is compatible/assignable (String->Object we want to keep String as transformed DataType)
        Class<?> type = payloadTransformedClass != null && transformed.getType() == Object.class && original.isCompatibleWith(DataTypeFactory.create(payloadTransformedClass, mimeType)) ? original.getType() : transformed.getType();

        DataType mergedDataType = DataTypeFactory.create(type, mimeType);
        mergedDataType.setEncoding(encoding);
        return mergedDataType;
    }

    protected void setDataType(DataType<?> dt)
    {
        dataType = dt.cloneDataType();

        setEncoding(dt == null ? null : dt.getEncoding());
        setMimeType(dt == null ? null : dt.getMimeType());
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
                    throw newException("Thread with name " + Thread.currentThread().getName() + " is trying to access to the message with hashcode " + System.identityHashCode(this) + " and id " + this.id
                                       + ". However its owner is " + ownerThread.get());
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
     * @deprecated since 3.8.0. Use {@link ClassUtils#isConsumable(Class)} instead.
     * 
     * Determines if the payload of this message is consumable i.e. it can't be read
     * more than once.
     */
    @Deprecated
    public boolean isConsumable()
    {
        return ClassUtils.isConsumable(getPayload().getClass());
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
                        DataType source = DataTypeFactory.createFromObject(theContent);
                        Transformer transformer = muleContext.getRegistry().lookupTransformer(source, DataType.BYTE_ARRAY_DATA_TYPE);
                        if (transformer == null)
                        {
                            throw new TransformerException(CoreMessages.noTransformerFoundForMessage(source, DataType.BYTE_ARRAY_DATA_TYPE));
                        }
                        contents = transformer.transform(theContent);
                    }
                    catch(TransformerException ex)
                    {
                        String message = format(
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
            new DataOutputStream(out).write(serializablePayload);
        }
        out.writeObject(serializeAttachments(inboundAttachments));
        out.writeObject(serializeAttachments(outboundAttachments));

        // TODO: we don't serialize the originalPayload for now
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

        boolean payloadWasSerialized = in.readBoolean();
        if (payloadWasSerialized)
        {
            payload = in.readObject();
        }
        else
        {
            int payloadSize = in.readInt();
            byte[] serializedPayload = new byte[payloadSize];
            new DataInputStream(in).readFully(serializedPayload);
            payload = serializedPayload;
        }
        inboundAttachments = deserializeAttachments((Map<String, SerializedDataHandler>)in.readObject());
        outboundAttachments = deserializeAttachments((Map<String, SerializedDataHandler>)in.readObject());
    }

    /**
     * Invoked after deserialization. This is called when the marker interface
     * {@link org.mule.util.store.DeserializationPostInitialisable} is used. This will get invoked
     * after the object has been deserialized passing in the current mulecontext when using either
     * {@link org.mule.transformer.wire.SerializationWireFormat},
     * {@link org.mule.transformer.wire.SerializedMuleMessageWireFormat} or the
     * {@link org.mule.transformer.simple.ByteArrayToSerializable} transformer.
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
    public DataType<?> getDataType()
    {
        return dataType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
    @Deprecated
    public void setStringProperty(String name, String value)
    {
        assertAccess(WRITE);
        logger.warn("MuleMessage.setStringProperty() method is deprecated, use MuleMessage.setOutboundProperty() instead.  This method will be removed in the next point release");
        setOutboundProperty(name, value);
    }

    @Override
    public DataType<?> getPropertyDataType(String name, PropertyScope scope)
    {
        return properties.getPropertyDataType(name, scope);
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

    @Override
    public MuleMessage createInboundMessage() throws Exception
    {
        DefaultMuleMessage newMessage =  new DefaultMuleMessage(this);
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

        for (Map.Entry<String, Object> s : newInboundProperties.entrySet())
        {
            DataType<?> propertyDataType = getPropertyDataType(s.getKey(), PropertyScope.OUTBOUND);

            newMessage.setInboundProperty(s.getKey(), s.getValue(), propertyDataType);
        }

        newMessage.inboundAttachments.clear();
        newMessage.outboundAttachments.clear();

        for (Map.Entry<String, DataHandler> s : attachments.entrySet())
        {
            newMessage.addInboundAttachment(s.getKey(), s.getValue());
        }

        newMessage.setCorrelationId(getCorrelationId());
        newMessage.setCorrelationGroupSize(getCorrelationGroupSize());
        newMessage.setCorrelationSequence(getCorrelationSequence());
        newMessage.setReplyTo(getReplyTo());
        newMessage.setEncoding(getEncoding());
    }
    
    void setSessionProperties(Map<String, TypedValue> sessionProperties)
    {
        properties.sessionMap = sessionProperties;
    }

    void setInvocationProperties(Map<String, TypedValue> invocationProperties)
    {
        properties.invocationMap = invocationProperties;
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
    
    protected Map<String, TypedValue> getOrphanFlowVariables()
    {
        return properties.getOrphanFlowVariables();
    }
}
