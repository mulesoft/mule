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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.MuleProperties;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UniqueIdNotSupportedException;

import javax.activation.DataHandler;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <code>AbstractMessageAdapter</code> provides a base implementation for
 * simple message types that maybe don't normally allow for meta information,
 * such as File or tcp.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractMessageAdapter implements UMOMessageAdapter {

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected Map properties = new HashMap();
    protected Map attachments = new HashMap();

    protected UMOExceptionPayload exceptionPayload;

    /**
     * Removes an associated property from the message
     *
     * @param key the key of the property to remove
     */
    public Object removeProperty(Object key) {
        Object prop = properties.get(key);
        if (prop != null) {
            properties.remove(key);
        }
        return prop;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#getProperty(java.lang.Object)
     */
    public Object getProperty(Object key) {
        return properties.get(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#getPropertyNames()
     */
    public Iterator getPropertyNames() {
        return properties.keySet().iterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.providers.UMOMessageAdapter#setProperty(java.lang.Object,
     *      java.lang.Object)
     */
    public void setProperty(Object key, Object value) {
        properties.put(key, value);
    }

    public String getUniqueId() {
        throw new UniqueIdNotSupportedException(this);
    }

    public Object getProperty(String name, Object defaultValue) {
        Object result = properties.get(name);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

    public int getIntProperty(String name, int defaultValue) {
        Object result = properties.get(name);
        if (result != null) {
            if (result instanceof Integer) {
                return ((Integer) result).intValue();
            } else {
                try {
                    return Integer.parseInt(result.toString());
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        } else {
            return defaultValue;
        }
    }

    public long getLongProperty(String name, long defaultValue) {
        Object result = properties.get(name);
        if (result != null) {
            if (result instanceof Long) {
                return ((Long) result).longValue();
            } else {
                try {
                    return Long.parseLong(result.toString());
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        } else {
            return defaultValue;
        }
    }

    public double getDoubleProperty(String name, double defaultValue) {
        Object result = properties.get(name);
        if (result != null) {
            if (result instanceof Double) {
                return ((Double) result).doubleValue();
            } else {
                try {
                    return Double.parseDouble(result.toString());
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        } else {
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(String name, boolean defaultValue) {
        Object result = properties.get(name);
        if (result != null) {
            if (result instanceof Boolean) {
                return ((Boolean) result).booleanValue();
            } else {
                return Boolean.valueOf(result.toString()).booleanValue();
            }
        } else {
            return defaultValue;
        }
    }

    public void setBooleanProperty(String name, boolean value) {
        properties.put(name, Boolean.valueOf(value));
    }

    public void setIntProperty(String name, int value) {
        properties.put(name, new Integer(value));
    }

    public void setLongProperty(String name, long value) {
        properties.put(name, new Long(value));
    }

    public void setDoubleProperty(String name, double value) {
        properties.put(name, new Double(value));
    }

    public Object getReplyTo() {
        return getProperty(MuleProperties.MULE_REPLY_TO_PROPERTY);
    }

    public void setReplyTo(Object replyTo) {
        setProperty(MuleProperties.MULE_REPLY_TO_PROPERTY, replyTo);
    }

    public String getCorrelationId() {
        return (String) getProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
    }

    public void setCorrelationId(String correlationId) {
        setProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY, correlationId);
    }

    /**
     * Gets the sequence or ordering number for this message in the the
     * correlation group (as defined by the correlationId)
     *
     * @return the sequence number or -1 if the sequence is not important
     */
    public int getCorrelationSequence() {
        return getIntProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, -1);

    }

    /**
     * Gets the sequence or ordering number for this message in the the
     * correlation group (as defined by the correlationId)
     *
     * @param sequence the sequence number or -1 if the sequence is not
     *                 important
     */
    public void setCorrelationSequence(int sequence) {
        setIntProperty(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, sequence);
    }

    /**
     * Determines how many messages are in the correlation group
     *
     * @return total messages in this group or -1 if the size is not known
     */
    public int getCorrelationGroupSize() {
        return getIntProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, -1);
    }

    /**
     * Determines how many messages are in the correlation group
     *
     * @param size the total messages in this group or -1 if the size is not
     *             known
     */
    public void setCorrelationGroupSize(int size) {
        setIntProperty(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, size);
    }

    public UMOExceptionPayload getExceptionPayload() {
        return exceptionPayload;
    }

    public void setExceptionPayload(UMOExceptionPayload payload) {
        exceptionPayload = payload;
    }

    public void addAttachment(String name, DataHandler dataHandler) throws Exception {
        attachments.put(name, dataHandler);
    }

    public void removeAttachment(String name) throws Exception {
        attachments.remove(name);
    }

    public DataHandler getAttachment(String name) {
        return (DataHandler) attachments.get(name);
    }

    public Set getAttachmentNames() {
        return attachments.keySet();
    }
}
