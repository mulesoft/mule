/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.message;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.UMOMessage;

import java.util.Map;

/**
 * <code>BaseMessage</code> A default message implementation
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class BaseMessage extends AbstractMessageAdapter implements UMOMessage
{
    protected Object message;

    public BaseMessage(Object message)
    {
        this.message = message;
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
        properties.putAll(properties);
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
}
