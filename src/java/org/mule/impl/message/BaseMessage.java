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
package org.mule.impl.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>BaseMessage</code> A default message implementation used for messages
 * sent over the wire. client messages should NOT implement UMOMessage.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class BaseMessage implements Serializable
{
    protected Object message;

    protected Map context;

    public BaseMessage(Object message)
    {
        this.message = message;
        context = new HashMap();
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString() throws Exception
    {
        return message.toString();
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @return String representation of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return getPayloadAsString().getBytes();
    }

    /**
     * @return the current message
     */
    public Object getPayload()
    {
        return message;
    }

    /**
     * Adds a map of properties to associated with this message
     * 
     * @param properties the properties add to this message
     */
    public void addProperties(Map properties)
    {
        context.putAll(properties);
    }

    /**
     * Removes all properties on this message
     */
    public void clearProperties()
    {
        context.clear();
    }

    /**
     * Returns a map of all properties on this message
     * 
     * @return a map of all properties on this message
     */
    public Map getProperties()
    {
        return context;
    }

    public void setProperty(Object key, Object value)
    {
        context.put(key, value);
    }

    public Object getProperty(Object key)
    {
        return context.get(key);
    }
}
