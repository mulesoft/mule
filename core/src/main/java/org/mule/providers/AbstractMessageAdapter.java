/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.config.MuleManifest;
import org.mule.MuleRuntimeException;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.ThreadSafeAccess;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.FileUtils;
import org.mule.util.MapUtils;
import org.mule.util.StringUtils;
import org.mule.util.UUID;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractMessageAdapter</code> provides a base implementation for simple
 * message types that maybe don't normally allow for meta information, such as a File
 * or TCP.
 */
public abstract class AbstractMessageAdapter implements UMOMessageAdapter, ThreadSafeAccess
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected ConcurrentMap properties = new ConcurrentHashMap();
    protected ConcurrentMap attachments = new ConcurrentHashMap();
    protected String encoding = FileUtils.DEFAULT_ENCODING;

    protected UMOExceptionPayload exceptionPayload;
    protected String id = UUID.getUUID();

    // these are transient because serisalisation generates a new instance
    // so we allow mutation again (and we can't serialize threads anyway)
    private transient AtomicReference ownerThread = null;
    private transient AtomicBoolean mutable = null;
    public static final boolean WRITE = true;
    public static final boolean READ = false;

    protected AbstractMessageAdapter()
    {
        // usual access for subclasses
    }

    protected AbstractMessageAdapter(UMOMessageAdapter template)
    {
        if (null != template)
        {
            Iterator propertyNames = template.getPropertyNames().iterator();
            while (propertyNames.hasNext())
            {
                String key = (String) propertyNames.next();
                try
                {
                   setProperty(key, template.getProperty(key));
                }
                catch (Exception e)
                {
                    throw new MuleRuntimeException(CoreMessages.failedToReadPayload(), e);
                }
            }
            Iterator attachmentNames = template.getAttachmentNames().iterator();
            while (attachmentNames.hasNext())
            {
                String key = (String) attachmentNames.next();
                try
                {
                    addAttachment(key, template.getAttachment(key));
                }
                catch (Exception e)
                {
                    throw new MuleRuntimeException(CoreMessages.failedToReadPayload(), e);
                }
            }
            encoding = template.getEncoding();
            exceptionPayload = template.getExceptionPayload();
            id = template.getUniqueId();
        }
    }

    public String toString()
    {
        assertAccess(READ);
        StringBuffer buf = new StringBuffer(120);
        buf.append(getClass().getName());
        buf.append("/" + super.toString());
        buf.append('{');
        buf.append("id=").append(getUniqueId());
        buf.append(", payload=").append(getPayload().getClass().getName());
        buf.append(", correlationId=").append(getCorrelationId());
        buf.append(", correlationGroup=").append(getCorrelationGroupSize());
        buf.append(", correlationSeq=").append(getCorrelationSequence());
        buf.append(", encoding=").append(getEncoding());
        buf.append(", exceptionPayload=").append(exceptionPayload);
        buf.append(", properties=").append(MapUtils.toString(properties, true));
        buf.append('}');
        return buf.toString();
    }

    public void addProperties(Map props)
    {
        assertAccess(WRITE);
        if (props != null)
        {
            synchronized (props)
            {
                for (Iterator iter = props.entrySet().iterator(); iter.hasNext();)
                {
                    Map.Entry entry = (Map.Entry) iter.next();
                    setProperty((String) entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public void clearProperties()
    {
        assertAccess(WRITE);
        properties.clear();
    }

    /**
     * Removes an associated property from the message
     * 
     * @param key the key of the property to remove
     */
    public Object removeProperty(String key)
    {
        assertAccess(WRITE);
        return properties.remove(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#getProperty(java.lang.Object)
     */
    public Object getProperty(String key)
    {
        assertAccess(READ);
        return properties.get(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#getPropertyNames()
     */
    public Set getPropertyNames()
    {
        assertAccess(READ);
        return Collections.unmodifiableSet(properties.keySet());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#setProperty(java.lang.Object,
     *      java.lang.Object)
     */
    public void setProperty(String key, Object value)
    {
        assertAccess(WRITE);
        if (key != null)
        {
            if (value != null)
            {
                properties.put(key, value);
            }
            else
            {
                logger.warn("setProperty(key, value) called with null value; removing key: " + key
                            + "; please report the following stack trace to " + MuleManifest.getDevListEmail(),
                    new Throwable());
                properties.remove(key);
            }
        }
        else
        {
            logger.warn("setProperty(key, value) ignored because of null key for object: " + value
                        + "; please report the following stack trace to " + MuleManifest.getDevListEmail(),
                new Throwable());
        }
    }

    public String getUniqueId()
    {
        assertAccess(READ);
        return id;
    }

    public Object getProperty(String name, Object defaultValue)
    {
        assertAccess(READ);
        return MapUtils.getObject(properties, name, defaultValue);
    }

    public int getIntProperty(String name, int defaultValue)
    {
        assertAccess(READ);
        return MapUtils.getIntValue(properties, name, defaultValue);
    }

    public long getLongProperty(String name, long defaultValue)
    {
        assertAccess(READ);
        return MapUtils.getLongValue(properties, name, defaultValue);
    }

    public double getDoubleProperty(String name, double defaultValue)
    {
        assertAccess(READ);
        return MapUtils.getDoubleValue(properties, name, defaultValue);
    }

    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        assertAccess(READ);
        return MapUtils.getBooleanValue(properties, name, defaultValue);
    }

    public String getStringProperty(String name, String defaultValue)
    {
        assertAccess(READ);
        return MapUtils.getString(properties, name, defaultValue);
    }

    public void setBooleanProperty(String name, boolean value)
    {
        assertAccess(WRITE);
        setProperty(name, Boolean.valueOf(value));
    }

    public void setIntProperty(String name, int value)
    {
        assertAccess(WRITE);
        setProperty(name, new Integer(value));
    }

    public void setLongProperty(String name, long value)
    {
        assertAccess(WRITE);
        setProperty(name, new Long(value));
    }

    public void setDoubleProperty(String name, double value)
    {
        assertAccess(WRITE);
        setProperty(name, new Double(value));
    }

    public void setStringProperty(String name, String value)
    {
        assertAccess(WRITE);
        setProperty(name, value);
    }

    public Object getReplyTo()
    {
        assertAccess(READ);
        return getProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
    }

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

    public String getCorrelationId()
    {
        assertAccess(READ);
        return (String) getProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
    }

    public void setCorrelationId(String correlationId)
    {
        assertAccess(WRITE);
        if (StringUtils.isNotBlank(correlationId))
        {
            setProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY, correlationId);
        }
        else
        {
            removeProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        }
    }

    /**
     * Gets the sequence or ordering number for this message in the the correlation
     * group (as defined by the correlationId)
     * 
     * @return the sequence number or -1 if the sequence is not important
     */
    public int getCorrelationSequence()
    {
        assertAccess(READ);
        return getIntProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, -1);
    }

    /**
     * Gets the sequence or ordering number for this message in the the correlation
     * group (as defined by the correlationId)
     * 
     * @param sequence the sequence number or -1 if the sequence is not important
     */
    public void setCorrelationSequence(int sequence)
    {
        assertAccess(WRITE);
        setIntProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, sequence);
    }

    /**
     * Determines how many messages are in the correlation group
     * 
     * @return total messages in this group or -1 if the size is not known
     */
    public int getCorrelationGroupSize()
    {
        assertAccess(READ);
        return getIntProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, -1);
    }

    /**
     * Determines how many messages are in the correlation group
     * 
     * @param size the total messages in this group or -1 if the size is not known
     */
    public void setCorrelationGroupSize(int size)
    {
        assertAccess(WRITE);
        setIntProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, size);
    }

    public UMOExceptionPayload getExceptionPayload()
    {
        assertAccess(READ);
        return exceptionPayload;
    }

    public void setExceptionPayload(UMOExceptionPayload payload)
    {
        assertAccess(WRITE);
        exceptionPayload = payload;
    }

    public void addAttachment(String name, DataHandler dataHandler) throws Exception
    {
        assertAccess(WRITE);
        attachments.put(name, dataHandler);
    }

    public void removeAttachment(String name) throws Exception
    {
        assertAccess(WRITE);
        attachments.remove(name);
    }

    public DataHandler getAttachment(String name)
    {
        assertAccess(READ);
        return (DataHandler) attachments.get(name);
    }

    public Set getAttachmentNames()
    {
        assertAccess(READ);
        return Collections.unmodifiableSet(attachments.keySet());
    }

    public String getEncoding()
    {
        assertAccess(READ);
        return encoding;
    }

    /**
     * Sets the encoding for this message
     * 
     * @param encoding the encoding to use
     */
    public void setEncoding(String encoding)
    {
        assertAccess(WRITE);
        this.encoding = encoding;
    }

    /**
     * Converts the message implementation into a String representation. If encoding
     * is required it will use the encoding set on the message
     * 
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public final String getPayloadAsString() throws Exception
    {
        assertAccess(READ);
        return getPayloadAsString(getEncoding());
    }

    protected byte[] convertToBytes(Object object) throws TransformerException, UnsupportedEncodingException
    {
        assertAccess(READ);
        if (object instanceof String)
        {
            return object.toString().getBytes(getEncoding());
        }

        if (object instanceof byte[])
        {
            return (byte[]) object;
        }
        else if (object instanceof Serializable)
        {
            try
            {
                return SerializationUtils.serialize((Serializable) object);
            }
            catch (Exception e)
            {
                throw new TransformerException(
                    CoreMessages.transformFailed(object.getClass().getName(), "byte[]"), e);
            }
        }
        else
        {
            throw new TransformerException(
                CoreMessages.transformOnObjectNotOfSpecifiedType(object.getClass().getName(), 
                    "byte[] or " + Serializable.class.getName()));
        }
    }

    /**
     * Restrict mutation to private use within a single thread.
     * Allow reading and writing by initial thread only.
     * Once accessed by another thread, no writing allowed at all.
     *
     * @param write
     */
    public void assertAccess(boolean write)
    {
        initAccessControl();
        setOwner();
        checkMutable(write);
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
        // MuleProperties.MULE_THREAD_UNSAFE_MESSAGES_PROPERTY (org.mule.disable.threadsafemessages)
        // (ie by adding -Dorg.mule.disable.threadsafemessages=true to the java command line).
        //
        // To remove the underlying cause, however, you probably need to do one of:
        //
        // - make sure that the message adapter you are using correclty implements the
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
            else
            {
                // access by another thread
                mutable.set(false);
            }
        }
    }

    protected IllegalStateException newException(String message)
    {
        IllegalStateException exception = new IllegalStateException(message);
        logger.error("Message access violation", exception);
        return exception;
    }

    protected boolean isDisabled()
    {
        return org.apache.commons.collections.MapUtils.getBooleanValue(System.getProperties(),
                MuleProperties.MULE_THREAD_UNSAFE_MESSAGES_PROPERTY, false);
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

    public synchronized void resetAccessControl()
    {
        assertAccess(WRITE);
        ownerThread.set(null);
        mutable.set(true);
    }

    public abstract ThreadSafeAccess newThreadCopy();

}
