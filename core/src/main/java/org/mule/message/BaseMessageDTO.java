/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.message;

import org.mule.transport.NullPayload;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>BaseMessage</code> A default message implementation used for messages sent
 * over the wire. client messages must NOT implement MuleMessage.
 */
public class BaseMessageDTO implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6105691921086093748L;

    private Object payload;

    protected Map<String, Object> properties;

    public BaseMessageDTO()
    {
        this(NullPayload.getInstance());
    }

    public BaseMessageDTO(Object payload)
    {
        this.payload = payload;
        properties = new HashMap<String, Object>();
    }


    public void setPayload(Object payload)
    {
        this.payload = payload;
    }

    /**
     * @return the current message
     */
    public Object getPayload()
    {
        return payload;
    }


    /**
     * Adds a map of properties to associated with this message
     *
     * @param properties the properties add to this message
     */
    public void addProperties(Map<String, Object> properties)
    {
        this.properties.putAll(properties);
    }

    /**
     * Removes all properties on this message
     */
    public void clearProperties()
    {
        properties.clear();
    }

    /**
     * Returns a map of all properties on this message
     *
     * @return a map of all properties on this message
     */
    public Map getProperties()
    {
        return properties;
    }

    public void setProperty(String key, Object value)
    {
        properties.put(key, value);
    }

    public Object getProperty(String key)
    {
        return properties.get(key);
    }

    public String toString()
    {
        return "BaseMessageDTO{" + "payload=" + payload + ", properties=" + properties + "}";
    }
}
