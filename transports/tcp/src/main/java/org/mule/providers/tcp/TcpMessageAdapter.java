/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.SerializationUtils;

/**
 * <code>TcpMessageAdapter</code> TODO
 */

public class TcpMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 7229837140160407794L;

    private Object message;

    public TcpMessageAdapter(Object message) throws MessagingException
    {
        if (message instanceof MuleMessage)
        {
            MuleMessage muleMessage = (MuleMessage)message;
            Set s = muleMessage.getPropertyNames();
            Iterator i = s.iterator();
            while (i.hasNext())
            {
                Object o = i.next();
                this.properties.put(o, muleMessage.getProperty(o.toString()));
            }
            this.message = muleMessage.getPayload();
        }
        else if (message instanceof Serializable)
        {
            this.message = message;
        }
        else
        {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @param encoding The encoding to use when transforming the message (if
     *            necessary). The parameter is used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception
    {
        if (message instanceof byte[])
        {
            return new String((byte[])message, encoding);
        }
        else
        {
            return message.toString();
        }
    }

    public byte[] getPayloadAsBytes() throws Exception
    {
        if (message instanceof byte[])
        {
            return (byte[])message;
        }
        else
        {
            return SerializationUtils.serialize((Serializable)message);
        }
    }

    public Object getPayload()
    {
        return message;
    }
}
