/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.MuleRuntimeException;
import org.mule.config.MuleManifest;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.ThreadSafeAccess;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.provider.PropertyScope;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.util.DebugOptions;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;
import org.mule.util.UUID;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;

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
     * Should we fail when we detect scribbling?  This can be overridden by setting the
     * property {@link org.mule.config.MuleProperties#MULE_THREAD_UNSAFE_MESSAGES_PROPERTY}
     */
    public static final boolean DEFAULT_FAILFAST = true;

    /** logger used by this class */
    protected static transient Log logger;

    /** Scoped properties for this message */
    protected MessagePropertiesContext properties = new MessagePropertiesContext();

    /** Collection of attachments associatated with this message */
    protected ConcurrentMap attachments = new ConcurrentHashMap();

    /** The encoding used by this message. This is usually used when working with String representations of the message payload */
    protected String encoding = FileUtils.DEFAULT_ENCODING;

    /** If an excpetion occurs while processing this message an exception payload will be attached here */
    protected UMOExceptionPayload exceptionPayload;

    /** the default UUID for the message. If the underlying transport has the notion of a message id, this uuid will be ignorred */
    protected String id = UUID.getUUID();

    // these are transient because serisalisation generates a new instance
    // so we allow mutation again (and we can't serialize threads anyway)
    private transient AtomicReference ownerThread = null;

    private transient AtomicBoolean mutable = null;

    protected AbstractMessageAdapter()
    {
        // usual access for subclasses
        logger = LogFactory.getLog(getClass());
    }

    /**
     * Creates a message adapter copying values from an existing one
     * @param template
     */
    protected AbstractMessageAdapter(UMOMessageAdapter template)
    {
        logger = LogFactory.getLog(getClass());
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
        if (logger.isDebugEnabled())
        {
            buf.append(", properties=").append(properties);
        }
        buf.append('}');
        return buf.toString();
    }

    /** {@inheritDoc} */
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

    /**
     * A convenience method for extending classes to Set inbound scoped properties on the message
     * properties that arrive on the inbound message should be set as inbound-scoped properties. These are
     * read-only
     * @param props the properties to set
     * @see org.mule.umo.provider.PropertyScope
     */
    protected void addInboundProperties(Map props)
    {
        properties.addInboundProperties(props);
    }

    /** {@inheritDoc} */
    public void clearProperties()
    {
        assertAccess(WRITE);
        properties.clearProperties();
    }

    /** {@inheritDoc} */
    public Object removeProperty(String key)
    {
        assertAccess(WRITE);
        return properties.removeProperty(key);
    }

    /** {@inheritDoc} */
    public Object getProperty(String key)
    {
        assertAccess(READ);
        return properties.getProperty(key);
    }

    /** {@inheritDoc} */
    public Set getPropertyNames()
    {
        assertAccess(READ);
        return properties.getPropertyNames();
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    public String getUniqueId()
    {
        assertAccess(READ);
        return id;
    }

    /** {@inheritDoc} */
    public Object getProperty(String name, Object defaultValue)
    {
        assertAccess(READ);
        return properties.getProperty(name, defaultValue);
    }

    /** {@inheritDoc} */
    public int getIntProperty(String name, int defaultValue)
    {
        assertAccess(READ);
        return properties.getIntProperty(name, defaultValue);
    }

    /** {@inheritDoc} */
    public long getLongProperty(String name, long defaultValue)
    {
        assertAccess(READ);
        return properties.getLongProperty(name, defaultValue);
    }

    /** {@inheritDoc} */
    public double getDoubleProperty(String name, double defaultValue)
    {
        assertAccess(READ);
        return properties.getDoubleProperty(name, defaultValue);
    }

    /** {@inheritDoc} */
    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        assertAccess(READ);
        return properties.getBooleanProperty(name, defaultValue);
    }

    /** {@inheritDoc} */
    public String getStringProperty(String name, String defaultValue)
    {
        assertAccess(READ);
        return properties.getStringProperty(name, defaultValue);
    }

    /** {@inheritDoc} */
    public void setBooleanProperty(String name, boolean value)
    {
        assertAccess(WRITE);
        setProperty(name, Boolean.valueOf(value));
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    public void setStringProperty(String name, String value)
    {
        assertAccess(WRITE);
        setProperty(name, value);
    }

    /** {@inheritDoc} */
    public Object getReplyTo()
    {
        assertAccess(READ);
        return getProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    public String getCorrelationId()
    {
        assertAccess(READ);
        return (String) getProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
    }


    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    public int getCorrelationSequence()
    {
        assertAccess(READ);
        return getIntProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, -1);
    }

    /** {@inheritDoc} */
    public void setCorrelationSequence(int sequence)
    {
        assertAccess(WRITE);
        setIntProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, sequence);
    }

    /** {@inheritDoc} */
    public int getCorrelationGroupSize()
    {
        assertAccess(READ);
        return getIntProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, -1);
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    public void setExceptionPayload(UMOExceptionPayload payload)
    {
        assertAccess(WRITE);
        exceptionPayload = payload;
    }

    /** {@inheritDoc} */
    public void addAttachment(String name, DataHandler dataHandler) throws Exception
    {
        assertAccess(WRITE);
        attachments.put(name, dataHandler);
    }

    /** {@inheritDoc} */
    public void removeAttachment(String name) throws Exception
    {
        assertAccess(WRITE);
        attachments.remove(name);
    }

    /** {@inheritDoc} */
    public DataHandler getAttachment(String name)
    {
        assertAccess(READ);
        return (DataHandler) attachments.get(name);
    }

    /** {@inheritDoc} */
    public Set getAttachmentNames()
    {
        assertAccess(READ);
        return Collections.unmodifiableSet(attachments.keySet());
    }

    /** {@inheritDoc} */
    public String getEncoding()
    {
        assertAccess(READ);
        return encoding;
    }

    /** {@inheritDoc} */
    public void setEncoding(String encoding)
    {
        assertAccess(WRITE);
        this.encoding = encoding;
    }

    /** {@inheritDoc} */
    public void release()
    {
        //TODO handle other stream types
        if(getPayload() instanceof InputStream)
        {
            IOUtils.closeQuietly((InputStream)getPayload());
        }
        properties.clearProperties();
        attachments.clear();
    }

    ///////////////////////// ThreadSafeAccess impl /////////////////////////////////////

    /** {@inheritDoc} */
    public void assertAccess(boolean write)
    {
        if(DebugOptions.isAssertMessageAccess())
        {
            initAccessControl();
            setOwner();
            checkMutable(write);
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
        logger.warn("Message access violation", exception);
        return exception;
    }

    protected boolean isDisabled()
    {
        return org.apache.commons.collections.MapUtils.getBooleanValue(System.getProperties(),
                MuleProperties.MULE_THREAD_UNSAFE_MESSAGES_PROPERTY, !DEFAULT_FAILFAST);
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

    /** {@inheritDoc} */
    public synchronized void resetAccessControl()
    {
        if(DebugOptions.isAssertMessageAccess())
        {
            assertAccess(WRITE);
            ownerThread.set(null);
            mutable.set(true);
        }
    }

    /** {@inheritDoc} */
    public ThreadSafeAccess newThreadCopy()
    {
        if (logger.isInfoEnabled())
        {
            logger.info("The newThreadCopy method in AbstractMessageAdapter is being used directly. "
                    + "This code may be susceptible to 'scribbling' issues with messages. "
                    + "Please consider implementing the ThreadSafeAccess interface in the message adapter.");
        }
        return this;
    }

}
