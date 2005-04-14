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
 */
package org.mule.impl;

import org.mule.providers.DefaultMessageAdapter;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.UMOMessage;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UniqueIdNotSupportedException;
import org.mule.util.PropertiesHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>MuleMessage</code> is a wrapper that contains a payload payload and properties
 * associated with the payload.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MuleMessage implements UMOMessage
{
    private UMOMessageAdapter adapter;

    protected UMOExceptionPayload exceptionPayload;


    public MuleMessage(UMOMessageAdapter message)
    {
        adapter = message;
    }

    public MuleMessage(Object message, Map props)
    {
        if(message instanceof UMOMessageAdapter) {
            adapter = (UMOMessageAdapter)message;
        } else {
            adapter = new DefaultMessageAdapter(message);
        }
        addProperties(props);
    }

    public UMOMessageAdapter getAdapter() {
        return adapter;
    }


    /**
     * Gets a property of the payload implementation
     *
     * @param key the key on which to lookup the property value
     * @return the property value or null if the property does not exist
     */
    public Object getProperty(Object key)
    {
        return adapter.getProperty(key.toString());
    }

    public Object removeProperty(Object key)
    {
        return adapter.removeProperty(key);
    }


    /**
     * Gets a property of the payload implementation
     *
     * @param key   the key on which to associate the value
     * @param value the property value
     */
    public void setProperty(Object key, Object value)
    {
        adapter.setProperty(key.toString(), value);
    }

    /**
     * Converts the payload implementation into a String representation
     *
     * @return String representation of the payload
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public String getPayloadAsString() throws Exception
    {
        return adapter.getPayloadAsString();
    }

    /**
     * @return all properties on this payload
     */
    public Iterator getPropertyNames()
    {
        return adapter.getPropertyNames();
    }

    /**
     * Converts the payload implementation into a String representation
     *
     * @return String representation of the payload
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return adapter.getPayloadAsBytes();
    }

    /**
     * @return the current payload
     */
    public Object getPayload()
    {
        return adapter.getPayload();
    }

    public void addProperties(Map properties)
    {
        if(properties != null)  {
            Map.Entry entry;
            for(Iterator iter = properties.entrySet().iterator(); iter.hasNext();) {
                entry = (Map.Entry)iter.next();
                adapter.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    public Map getProperties()
    {
        Map props = new HashMap();
        Object key;
        for(Iterator iter = getPropertyNames(); iter.hasNext();) {
            key = iter.next();
            props.put(key, getProperty(key));
        }
        return Collections.unmodifiableMap(props);
    }

    public void clearProperties()
    {
        for(Iterator iter = adapter.getPropertyNames(); iter.hasNext();) {
            Object key = iter.next();
            adapter.removeProperty(key);
            }
    }


    /**
     * Gets a double property from the event
     *
     * @param name         the name or key of the property
     * @param defaultValue a default value if the property doesn't exist in the event
     * @return the property value or the defaultValue if the property does not
     *         exist
     */
    public double getDoubleProperty(String name, double defaultValue)
    {
        return adapter.getDoubleProperty(name, defaultValue);
    }

    /**
     * Sets a double property on the event
     *
     * @param name  the property name or key
     * @param value the property value
     */
    public void setDoubleProperty(String name, double value)
    {
        adapter.setDoubleProperty(name, value);
    }

    public String getUniqueId() throws UniqueIdNotSupportedException
    {
        return adapter.getUniqueId();
    }

    public Object getProperty(String name, Object defaultValue)
    {
        return adapter.getProperty(name, defaultValue);
    }

    public int getIntProperty(String name, int defaultValue)
    {
        return adapter.getIntProperty(name, defaultValue);
    }

    public long getLongProperty(String name, long defaultValue)
    {
        return adapter.getLongProperty(name, defaultValue);
    }

    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        return adapter.getBooleanProperty(name, defaultValue);
    }

    public void setBooleanProperty(String name, boolean value)
    {
        adapter.setBooleanProperty(name, value);
    }

    public void setIntProperty(String name, int value)
    {
        adapter.setIntProperty(name, value);
    }

    public void setLongProperty(String name, long value)
    {
        adapter.setLongProperty(name, value);
    }

    /**
     * Sets a correlationId for this message.  The correlation Id can
     * be used by components in the system to manage message relations
     * <p/>
     * transport protocol.  As such not all messages will support the notion
     * of a correlationId i.e. tcp or file.  In this situation the correlation Id
     * is set as a property of the message where it's up to developer to keep
     * the association with the message. For example if the message is serialised to
     * xml the correlationId will be available in the message.
     *
     * @param id the Id reference for this relationship
     */
    public void setCorrelationId(String id)
    {
        adapter.setCorrelationId(id);
    }

    /**
     * Sets a correlationId for this message.  The correlation Id can
     * be used by components in the system to manage message relations.
     * <p/>
     * The correlationId is associated with the message using the underlying
     * transport protocol.  As such not all messages will support the notion
     * of a correlationId i.e. tcp or file.  In this situation the correlation Id
     * is set as a property of the message where it's up to developer to keep
     * the association with the message. For example if the message is serialised to
     * xml the correlationId will be available in the message.
     *
     * @return the correlationId for this message or null if one hasn't been set
     */
    public String getCorrelationId()
    {
        return adapter.getCorrelationId();
    }

    /**
     * Sets a replyTo address for this message.  This is useful in an asynchronous
     * environment where the caller doesn't wait for a response and the response needs
     * to be routed somewhere for further processing.
     * The value of this field can be any valid endpointUri url.
     *
     * @param replyTo the endpointUri url to reply to
     */
    public void setReplyTo(Object replyTo)
    {
        adapter.setReplyTo(replyTo);
    }

    /**
     * Sets a replyTo address for this message.  This is useful in an asynchronous
     * environment where the caller doesn't wait for a response and the response needs
     * to be routed somewhere for further processing.
     * The value of this field can be any valid endpointUri url.
     *
     * @return the endpointUri url to reply to or null if one has not been set
     */
    public Object getReplyTo()
    {
        return adapter.getReplyTo();
    }

    /**
     * Gets the sequence or ordering number for this message in the
     * the correlation group (as defined by the correlationId)
     *
     * @return the sequence number  or -1 if the sequence is not important
     */
    public int getCorrelationSequence()
    {
        return adapter.getCorrelationSequence();
    }

    /**
     * Gets the sequence or ordering number for this message in the
     * the correlation group (as defined by the correlationId)
     *
     * @param sequence the sequence number  or -1 if the sequence is not important
     */
    public void setCorrelationSequence(int sequence)
    {
        adapter.setCorrelationSequence(sequence);
    }

    /**
     * Determines how many messages are in the correlation group
     *
     * @return total messages in this group or -1 if the size is not known
     */
    public int getCorrelationGroupSize()
    {
        return adapter.getCorrelationGroupSize();
    }

    /**
     * Determines how many messages are in the correlation group
     *
     * @param size the total messages in this group or -1 if the size is not known
     */
    public void setCorrelationGroupSize(int size)
    {
        adapter.setCorrelationGroupSize(size);
    }

    public UMOExceptionPayload getExceptionPayload() {
        return exceptionPayload;
    }

    public void setExceptionPayload(UMOExceptionPayload exceptionPayload) {
        this.exceptionPayload = exceptionPayload;
    }


    public String toString() {

        String id = null;
        try {
            id = getUniqueId();
        } catch (UniqueIdNotSupportedException e) {
            id = "[uniquieId not supported]";
        }
        StringBuffer buf = new StringBuffer();
        buf.append("MuleMessage{");
        buf.append("id=").append(id);
        buf.append(", payload=").append(getPayload().getClass().getName());
        buf.append(", correlationId=").append(getCorrelationId());
        buf.append(", correlation Group=").append(getCorrelationGroupSize());
        buf.append(", correlation Seq=").append(getCorrelationSequence());
        buf.append(", exception Payload=").append(exceptionPayload);
        buf.append("/n").append(PropertiesHelper.propertiesToString(getProperties(), true));
        buf.append("}");
        return buf.toString();
    }
}
