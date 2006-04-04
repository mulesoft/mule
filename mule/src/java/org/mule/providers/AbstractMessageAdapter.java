/*
 * $Header$
 * $Revision$
 * $Date$
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

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UniqueIdNotSupportedException;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.PropertiesHelper;

import javax.activation.DataHandler;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

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

    protected Map properties = new ConcurrentHashMap();
    protected Map attachments = new ConcurrentHashMap();
    protected String encoding = MuleManager.getConfiguration().getEncoding();

    protected UMOExceptionPayload exceptionPayload;

    public String toString()
    {
        String id;

        try {
            id = getUniqueId();
        } catch (UniqueIdNotSupportedException e) {
            id = "[uniqueId not supported]";
        }

        StringBuffer buf = new StringBuffer(120);
        buf.append(getClass().getName());
        buf.append('{');
        buf.append("id=").append(id);
        buf.append(", payload=").append(getPayload().getClass().getName());
        buf.append(", correlationId=").append(getCorrelationId());
        buf.append(", correlationGroup=").append(getCorrelationGroupSize());
        buf.append(", correlationSeq=").append(getCorrelationSequence());
        buf.append(", encoding=").append(getEncoding());
        buf.append(", exceptionPayload=").append(exceptionPayload);
        buf.append(", properties=").append(PropertiesHelper.propertiesToString(properties, true));
        buf.append('}');
        return buf.toString();
    }

    public void addProperties(Map props)
    {
        if (props != null) {
            synchronized(props) {
                for (Iterator iter = props.entrySet().iterator(); iter.hasNext();) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    setProperty(entry.getKey(), entry.getValue());
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
    public Object removeProperty(Object key)
    {
        return properties.remove(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#getProperty(java.lang.Object)
     */
    public Object getProperty(Object key)
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
    public void setProperty(Object key, Object value)
    {
        if (key != null) {
            if (value != null) {
                properties.put(key, value);
            }
            else {
                properties.remove(key);
            }
        }
    }

    public String getUniqueId()
    {
        throw new UniqueIdNotSupportedException(this);
    }

    public Object getProperty(String name, Object defaultValue)
    {
        return PropertiesHelper.getProperty(properties, name, defaultValue);
    }

    public int getIntProperty(String name, int defaultValue)
    {
        return PropertiesHelper.getIntProperty(properties, name, defaultValue);
    }

    public long getLongProperty(String name, long defaultValue)
    {
        return PropertiesHelper.getLongProperty(properties, name, defaultValue);
    }

    public double getDoubleProperty(String name, double defaultValue)
    {
        return PropertiesHelper.getDoubleProperty(properties, name, defaultValue);
    }

    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        return PropertiesHelper.getBooleanProperty(properties, name, defaultValue);
    }

    public String getStringProperty(String name, String defaultValue)
    {
        return PropertiesHelper.getStringProperty(properties, name, defaultValue);
    }

    public void setBooleanProperty(String name, boolean value)
    {
        properties.put(name, Boolean.valueOf(value));
    }

    public void setIntProperty(String name, int value)
    {
        properties.put(name, new Integer(value));
    }

    public void setLongProperty(String name, long value)
    {
        properties.put(name, new Long(value));
    }

    public void setDoubleProperty(String name, double value)
    {
        properties.put(name, new Double(value));
    }

    public void setStringProperty(String name, String value)
    {
        properties.put(name, value);
    }

    public Object getReplyTo()
    {
        return getProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
    }

    public void setReplyTo(Object replyTo)
    {
        setProperty(MuleProperties.MULE_REPLY_TO_PROPERTY, replyTo);
    }

    public String getCorrelationId()
    {
        return (String)getProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
    }

    public void setCorrelationId(String correlationId)
    {
        setProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY, correlationId);
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
