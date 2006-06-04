/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */

package org.mule.providers;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;

import javax.activation.DataHandler;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.PropertiesUtils;
import org.mule.util.UUID;

/**
 * <code>AbstractMessageAdapter</code> provides a base implementation for
 * simple message types that maybe don't normally allow for meta information,
 * such as File or tcp.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractMessageAdapter implements UMOMessageAdapter
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected ConcurrentMap properties = new ConcurrentHashMap();
    protected ConcurrentMap attachments = new ConcurrentHashMap();
    protected String encoding = MuleManager.getConfiguration().getEncoding();

    protected UMOExceptionPayload exceptionPayload;
    protected String id = UUID.getUUID();

    public String toString()
    {
        StringBuffer buf = new StringBuffer(120);
        buf.append(getClass().getName());
        buf.append('{');
        buf.append("id=").append(getUniqueId());
        buf.append(", payload=").append(getPayload().getClass().getName());
        buf.append(", correlationId=").append(getCorrelationId());
        buf.append(", correlationGroup=").append(getCorrelationGroupSize());
        buf.append(", correlationSeq=").append(getCorrelationSequence());
        buf.append(", encoding=").append(getEncoding());
        buf.append(", exceptionPayload=").append(exceptionPayload);
        buf.append(", properties=").append(PropertiesUtils.propertiesToString(properties, true));
        buf.append('}');
        return buf.toString();
    }

    public void addProperties(Map props)
    {
        if (props != null) {
            synchronized(props) {
                for (Iterator iter = props.entrySet().iterator(); iter.hasNext();) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    setProperty((String)entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public void clearProperties()
    {
        properties.clear();
    }

    /**
     * Removes an associated property from the message
     * 
     * @param key
     *            the key of the property to remove
     */
    public Object removeProperty(String key)
    {
        return properties.remove(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#getProperty(java.lang.Object)
     */
    public Object getProperty(String key)
    {
        return properties.get(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#getPropertyNames()
     */
    public Set getPropertyNames()
    {
        return properties.keySet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#setProperty(java.lang.Object,
     *      java.lang.Object)
     */
    public void setProperty(String key, Object value)
    {
        if (key != null) {
            if (value != null) {
                properties.put(key, value);
            }
            else {
                logger.warn("setProperty(key, value) called with null value; removing key: " + key
                        + "; please report the following stack trace to dev@mule.codehaus.org.", new Throwable());
                properties.remove(key);
            }
        } else {
            logger.warn("setProperty(key, value) ignored because of null key for object: " + value
                    + "; please report the following stack trace to dev@mule.codehaus.org.", new Throwable());
        }
    }

    public String getUniqueId()
    {
        return id;
    }

    public Object getProperty(String name, Object defaultValue)
    {
        return MapUtils.getObject(properties, name, defaultValue);
    }

    public int getIntProperty(String name, int defaultValue)
    {
        return MapUtils.getIntValue(properties, name, defaultValue);
    }

    public long getLongProperty(String name, long defaultValue)
    {
        return MapUtils.getLongValue(properties, name, defaultValue);
    }

    public double getDoubleProperty(String name, double defaultValue)
    {
        return MapUtils.getDoubleValue(properties, name, defaultValue);
    }

    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        return MapUtils.getBooleanValue(properties, name, defaultValue);
    }

    public String getStringProperty(String name, String defaultValue)
    {
        return MapUtils.getString(properties, name, defaultValue);
    }

    public void setBooleanProperty(String name, boolean value)
    {
        setProperty(name, Boolean.valueOf(value));
    }

    public void setIntProperty(String name, int value)
    {
        setProperty(name, new Integer(value));
    }

    public void setLongProperty(String name, long value)
    {
        setProperty(name, new Long(value));
    }

    public void setDoubleProperty(String name, double value)
    {
        setProperty(name, new Double(value));
    }

    public void setStringProperty(String name, String value)
    {
        setProperty(name, value);
    }

    public Object getReplyTo()
    {
        return getProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
    }

    public void setReplyTo(Object replyTo)
    {
        if (replyTo != null) {
            setProperty(MuleProperties.MULE_REPLY_TO_PROPERTY, replyTo);
        } else {
            removeProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
        }
    }

    public String getCorrelationId()
    {
        return (String)getProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
    }

    public void setCorrelationId(String correlationId)
    {
        if (StringUtils.isNotBlank(correlationId)) {
            setProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY, correlationId);
        } else {
            removeProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        }
    }

    /**
     * Gets the sequence or ordering number for this message in the the
     * correlation group (as defined by the correlationId)
     * 
     * @return the sequence number or -1 if the sequence is not important
     */
    public int getCorrelationSequence()
    {
        return getIntProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, -1);
    }

    /**
     * Gets the sequence or ordering number for this message in the the
     * correlation group (as defined by the correlationId)
     * 
     * @param sequence
     *            the sequence number or -1 if the sequence is not important
     */
    public void setCorrelationSequence(int sequence)
    {
        setIntProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, sequence);
    }

    /**
     * Determines how many messages are in the correlation group
     * 
     * @return total messages in this group or -1 if the size is not known
     */
    public int getCorrelationGroupSize()
    {
        return getIntProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, -1);
    }

    /**
     * Determines how many messages are in the correlation group
     * 
     * @param size
     *            the total messages in this group or -1 if the size is not
     *            known
     */
    public void setCorrelationGroupSize(int size)
    {
        setIntProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, size);
    }

    public UMOExceptionPayload getExceptionPayload()
    {
        return exceptionPayload;
    }

    public void setExceptionPayload(UMOExceptionPayload payload)
    {
        exceptionPayload = payload;
    }

    public void addAttachment(String name, DataHandler dataHandler) throws Exception
    {
        attachments.put(name, dataHandler);
    }

    public void removeAttachment(String name) throws Exception
    {
        attachments.remove(name);
    }

    public DataHandler getAttachment(String name)
    {
        return (DataHandler)attachments.get(name);
    }

    public Set getAttachmentNames()
    {
        return attachments.keySet();
    }

    public String getEncoding()
    {
        return encoding;
    }

    /**
     * Sets the encoding for this message
     *
     * @param encoding the encoding to use
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Converts the message implementation into a String representation. If
     * encoding is required it will use the encoding set on the message
     * 
     * @return String representation of the message payload
     * @throws Exception
     *             Implementation may throw an endpoint specific exception
     */
    public final String getPayloadAsString() throws Exception
    {
        return getPayloadAsString(getEncoding());
    }

    protected byte[] convertToBytes(Object object) throws TransformerException,
            UnsupportedEncodingException
    {
        if (object instanceof String) {
            return object.toString().getBytes(getEncoding());
        }

        if (object instanceof byte[]) {
            return (byte[])object;
        }
        else if (object instanceof Serializable) {
            try {
                return SerializationUtils.serialize((Serializable)object);
            }
            catch (Exception e) {
                throw new TransformerException(new Message(Messages.TRANSFORM_FAILED_FROM_X_TO_X, object
                        .getClass().getName(), "byte[]"), e);
            }
        }
        else {
            throw new TransformerException(new Message(Messages.TRANSFORM_ON_X_NOT_OF_SPECIFIED_TYPE_X,
                    object.getClass().getName(), "byte[] or " + Serializable.class.getName()));
        }
    }
}
