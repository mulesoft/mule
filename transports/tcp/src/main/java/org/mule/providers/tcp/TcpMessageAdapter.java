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
import org.mule.impl.ThreadSafeAccess;
import org.mule.providers.AbstractMessageAdapter;

import java.io.Serializable;
import java.util.Iterator;

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

    public TcpMessageAdapter(Object message)
    {
        if (message instanceof MuleMessage)
        {
            MuleMessage muleMessage = (MuleMessage)message;
            Iterator names = muleMessage.getPropertyNames().iterator();
            while (names.hasNext())
            {
                Object name = names.next();
                this.properties.put(name, muleMessage.getProperty(name.toString()));
            }
            this.message = muleMessage.getPayload();
        }
        else
        {
            this.message = message;
        }
    }

    protected TcpMessageAdapter(TcpMessageAdapter template)
    {
        super(template);
        message = template.message;
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @param encoding The encoding to use when transforming the message (if
     *         necessary). The parameter is used when converting from a byte array
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

    public ThreadSafeAccess newThreadCopy()
    {
        return new TcpMessageAdapter(this);
    }

}
