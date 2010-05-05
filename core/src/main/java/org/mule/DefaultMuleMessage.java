/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.PropertyScope;
import org.mule.config.MuleManifest;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;
import org.mule.transport.MessagePropertiesContext;
import org.mule.transport.NullPayload;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;
import org.mule.util.UUID;
import org.mule.util.store.DeserializationPostInitialisable;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;

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
     * If an excpetion occurs while processing this message an exception payload 
     * will be attached here 
     */
    private ExceptionPayload exceptionPayload;

    /** 
     * Scoped properties for this message 
     */
    private MessagePropertiesContext properties = new MessagePropertiesContext();

    /** 
     * Collection of attachments associatated with this message 
     */
    @SuppressWarnings("unchecked")
    private Map<String, DataHandler> attachments = new ConcurrentHashMap();

    private transient List<Integer> appliedTransformerHashCodes;
    private transient byte[] cache;
    protected transient MuleContext muleContext;

    // these are transient because serisalisation generates a new instance
    // so we allow mutation again (and we can't serialize threads anyway)
    private transient AtomicReference ownerThread = null;
    private transient AtomicBoolean mutable = null;

    static
    {
        addToConsuableClasses("javax.xml.stream.XMLStreamReader");
        addToConsuableClasses("javax.xml.transform.stream.StreamSource");
        consumableClasses.add(OutputHandler.class);
    }
    
    private static void addToConsuableClasses(String className)
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

    public DefaultMuleMessage(Object message, Map<String, Object> properties, MuleContext muleContext)
    {
        setMuleContext(muleContext);
        initAppliedTransformerHashCodes();

        // Explicitly check for MuleMessage as a safeguard since MuleMessage is instance of MessageAdapter
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
        addProperties(properties);
        resetAccessControl();
    }
    
    public DefaultMuleMessage(Object message, MuleMessage previous, MuleContext muleContext)
    {
        id = previous.getUniqueId();
        setMuleContext(muleContext);
        initAppliedTransformerHashCodes();
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
        // because the INBOUND property scope is immutable
        Map<String, Object> inboundProperties = 
            ((DefaultMuleMessage)muleMessage).properties.getScopedProperties(PropertyScope.INBOUND);
        addInboundProperties(inboundProperties);
        
        for (PropertyScope scope : PropertyScope.ALL_SCOPES)
        {
            try
            {
                for (String name : muleMessage.getPropertyNames(scope))
                {
                    Object value = muleMessage.getProperty(name);
                    setProperty(name, value, scope);
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
        if (previous.getAttachmentNames().size() > 0)
        {
            for (String name : previous.getAttachmentNames())
            {
                try
                {
                    addAttachment(name, previous.getAttachment(name));
                }
                catch (Exception e)
                {
                    throw new MuleRuntimeException(CoreMessages.failedToReadAttachment(name), e);
                }
            }
        }
    }

    private DefaultMuleMessage(DefaultMuleMessage message)
    {
        this(message.getPayload(), message, message.muleContext);
    }

    private void setMuleContext(MuleContext context)
    {
        if (context == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("muleContext").getMessage());
        }
        muleContext = context;
    }
    
    @SuppressWarnings("unchecked")
    private void initAppliedTransformerHashCodes()
    {
        appliedTransformerHashCodes = new CopyOnWriteArrayList();
    }

    /**
     * {@inheritDoc}
     */
    public <T> T getPayload(Class<T> outputType) throws TransformerException
    {
        return getPayload(outputType, getEncoding());
    }


    /**
     * {@inheritDoc}
     */
    public <T> T getPayload(DataType<T> outputType) throws TransformerException
    {
        return getPayload(outputType, getEncoding());
    }

    MuleContext getMuleContext()
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
     * @param encoding the encoding to use if required
     * @return The converted payload of this message. Note that this method will not alter the 
     *          payload of this message <b>unless</b> the payload is an {@link InputStream} in which
     *          case the stream will be read and the payload will become the fully read stream.
     * @throws TransformerException if a transformer cannot be found or there is an error during 
     *          transformation of the payload.
     * @since 3.0.0
     *
     * TODO make this public, roll encoding into the datatype object
     */
    @SuppressWarnings("unchecked")
    protected <T> T getPayload(DataType<T> resultType, String encoding) throws TransformerException
    {
        // Handle null by ignoring the request
        if (resultType == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("resultType").getMessage());
        }

        //TODO handling of mime type
        DataType<?> source = new DataTypeFactory().create(getPayload().getClass());

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
        
        // Pass in the adapter itself
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
     * Will attempt to obtain the payload of this message with the desired Class type. This will
     * try and resolve a trnsformr that can do this transformation. If a transformer cannot be found
     * an exception is thrown.  Any transfromers added to the reqgistry will be checked for compatability
     *
     * @param outputType the desired return type
     * @param encoding   the encoding to use if required
     * @return The converted payload of this message. Note that this method will not alter the payload of this
     *         message *unless* the payload is an inputstream in which case the stream will be read and the payload will become
     *         the fully read stream.
     * @throws TransformerException if a transformer cannot be found or there is an error during transformation of the
     *                              payload
     */
    protected <T> T getPayload(Class<T> outputType, String encoding) throws TransformerException
    {
        return (T) getPayload(new SimpleDataType(outputType), encoding);
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
     * @deprecated use {@link #getOriginalPayload()}
     */
    @Deprecated
    public Object getOrginalPayload()
    {
        return getOriginalPayload();
    }

    /**
     * {@inheritDoc}
     */
    public Object getOriginalPayload()
    {
        return originalPayload;
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
    public Object getProperty(String key)
    {
        assertAccess(READ);
        return properties.getProperty(key);
    }

    /**
     * {@inheritDoc}
     */
    public Object removeProperty(String key)
    {
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

    /** {@inheritDoc} */
    public void setProperty(String key, Object value)
    {
        assertAccess(WRITE);
        if (key != null)
        {
            if (value != null)
            {
                properties.setProperty(key, value);
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
    public byte[] getPayloadAsBytes() throws Exception
    {
        assertAccess(READ);
        if (cache != null)
        {
            return cache;
        }
        byte[] result = getPayload(byte[].class);
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
        String result = getPayload(String.class, encoding);
        if (muleContext.getConfiguration().isCacheMessageAsBytes())
        {
            cache = result.getBytes(encoding);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getPropertyNames()
    {
        assertAccess(READ);
        return properties.getPropertyNames();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getPropertyNames(PropertyScope scope)
    {
        assertAccess(READ);
        return properties.getScopedProperties(scope).keySet();
    }

    //** {@inheritDoc} */
    public double getDoubleProperty(String name, double defaultValue)
    {
        assertAccess(READ);
        return properties.getDoubleProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    public void setDoubleProperty(String name, double value)
    {
        assertAccess(WRITE);
        setProperty(name, Double.valueOf(value));
    }

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
        assertAccess(READ);
        return properties.getProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    public Object getProperty(String name, PropertyScope scope)
    {
        assertAccess(READ);
        return properties.getProperty(name, scope);
    }

    /**
     * {@inheritDoc}
     */
    public int getIntProperty(String name, int defaultValue)
    {
        assertAccess(READ);
        return properties.getIntProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    public long getLongProperty(String name, long defaultValue)
    {
        assertAccess(READ);
        return properties.getLongProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        assertAccess(READ);
        return properties.getBooleanProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    public void setBooleanProperty(String name, boolean value)
    {
        assertAccess(WRITE);
        setProperty(name, Boolean.valueOf(value));
    }

    /**
     * {@inheritDoc}
     */
    public void setIntProperty(String name, int value)
    {
        assertAccess(WRITE);
        setProperty(name, Integer.valueOf(value));
    }

    /**
     * {@inheritDoc}
     */
    public void setLongProperty(String name, long value)
    {
        assertAccess(WRITE);
        setProperty(name, Long.valueOf(value));
    }

    /**
     * {@inheritDoc}
     */
    public void setCorrelationId(String id)
    {
        assertAccess(WRITE);
        if (StringUtils.isNotBlank(id))
        {
            setProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY, id);
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
        return (String) getProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
    }

    /**
     * {@inheritDoc}
     */
    public void setReplyTo(Object replyTo)
    {
        assertAccess(WRITE);
        if (replyTo != null)
        {
            setProperty(MuleProperties.MULE_REPLY_TO_PROPERTY, replyTo);
        }
        else
        {
            removeProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getReplyTo()
    {
        assertAccess(READ);
        return getProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
    }

    /**
     * {@inheritDoc}
     */
    public int getCorrelationSequence()
    {
        assertAccess(READ);
        return getIntProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, -1);
    }

    /**
     * {@inheritDoc}
     */
    public void setCorrelationSequence(int sequence)
    {
        assertAccess(WRITE);
        setIntProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, sequence);
    }

    /**
     * {@inheritDoc}
     */
    public int getCorrelationGroupSize()
    {
        assertAccess(READ);
        return getIntProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, -1);
    }

    //** {@inheritDoc} */
    public void setCorrelationGroupSize(int size)
    {
        assertAccess(WRITE);
        setIntProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, size);
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
        buf.append(getClass().getName());
        buf.append("{id=").append(getUniqueId());
        buf.append(", payload=").append(getPayload().getClass().getName());
        buf.append(", properties=").append(properties);
        buf.append(", correlationId=").append(getCorrelationId());
        buf.append(", correlationGroup=").append(getCorrelationGroupSize());
        buf.append(", correlationSeq=").append(getCorrelationSequence());
        buf.append(", encoding=").append(getEncoding());
        buf.append(", exceptionPayload=").append(exceptionPayload);
        if (logger.isDebugEnabled())
        {
            buf.append(", properties=").append(properties);
        }
        buf.append('}');
        return buf.toString();
    }

    /**
     * {@inheritDoc}
     */
    public void addAttachment(String name, DataHandler dataHandler) throws Exception
    {
        assertAccess(WRITE);
        attachments.put(name, dataHandler);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAttachment(String name) throws Exception
    {
        assertAccess(WRITE);
        attachments.remove(name);
    }

    /**
     * {@inheritDoc}
     */
    public DataHandler getAttachment(String name)
    {
        assertAccess(READ);
        return attachments.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getAttachmentNames()
    {
        assertAccess(READ);
        return Collections.unmodifiableSet(attachments.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public String getEncoding()
    {
        assertAccess(READ);
        
        String encoding =  getStringProperty(MuleProperties.MULE_ENCODING_PROPERTY, null);
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
        setStringProperty(MuleProperties.MULE_ENCODING_PROPERTY, encoding);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringProperty(String name, String defaultValue)
    {
        assertAccess(READ);
        return properties.getStringProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    public void setStringProperty(String name, String value)
    {
        assertAccess(WRITE);
        setProperty(name, value);
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
        properties.clearProperties();
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
        appliedTransformerHashCodes.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void applyTransformers(List<? extends Transformer> transformers) throws TransformerException
    {
        applyTransformers(transformers, null);
    }

    /**
     * {@inheritDoc}
     */
    public void applyTransformers(Transformer... transformers) throws TransformerException
    {
        applyTransformers(Arrays.asList(transformers), null);
    }

    public void applyTransformers(List<? extends Transformer> transformers, Class<?> outputType) throws TransformerException
    {
        if (!transformers.isEmpty() && !appliedTransformerHashCodes.contains(transformers.hashCode()))
        {
            applyAllTransformers(transformers);
            appliedTransformerHashCodes.add(transformers.hashCode());
        }

        if (null != outputType && !getPayload().getClass().isAssignableFrom(outputType))
        {
            setPayload(getPayload(outputType));
        }
    }

    protected void applyAllTransformers(List transformers) throws TransformerException
    {
        if (!transformers.isEmpty())
        {
            for (Object transformer1 : transformers)
            {
                Transformer transformer = (Transformer) transformer1;

                if (getPayload() == null)
                {
                    if (transformer.isAcceptNull())
                    {
                        setPayload(NullPayload.getInstance());
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

                Class srcCls = getPayload().getClass();
                if (transformer.isSourceTypeSupported(srcCls))
                {
                    Object result = transformer.transform(this);

                    if (originalPayload == null && muleContext.getConfiguration().isCacheMessageOriginalPayload())
                    {
                        originalPayload = payload;
                    }

                    if (result instanceof MuleMessage)
                    {
                        synchronized (this)
                        {
                            MuleMessage resultMessage = (MuleMessage) result;
                            setPayload(resultMessage.getPayload());
                            originalPayload = resultMessage.getOrginalPayload();
                            // TODO MessageAdapterRemoval: copy attachments, properties here?
                        }
                    }
                    else
                    {
                        setPayload(result);
                    }
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
        // - make sure that the message adapter you are using correctly implements the
        // ThreadSafeAccess interface
        //
        // - make sure that dispatcher and receiver classes copy ThreadSafeAccess instances when
        // they are passed between threads

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

    public static MuleMessage copy(MuleMessage message)
    {
        if (message instanceof DefaultMuleMessage)
        {
            return new DefaultMuleMessage((DefaultMuleMessage) message);
        }
        else
        {
            //Very unlikely to happen unless a user des something odd
            throw new IllegalArgumentException("In order to clone a message it must be assignable from: " + DefaultMuleMessage.class.getName());
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
    void initAfterDeserialisation(MuleContext muleContext) throws MuleException
    {
        this.muleContext = muleContext;
    }

}
