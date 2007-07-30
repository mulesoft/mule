/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.stdio;

import org.mule.impl.ThreadSafeAccess;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.provider.MessageTypeNotSupportedException;

/**
 * <code>StdioMessageAdapter</code> TODO document
 */
public class StdioMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3094357859680956607L;

    // TODO shouldn't this be an Object, for handling at least byte[]s too?
    private final String message;

    public StdioMessageAdapter(Object message) throws MessageTypeNotSupportedException
    {
        if (message instanceof String)
        {
            this.message = (String)message;
        }
        else
        {
            throw new MessageTypeNotSupportedException(message, StdioMessageAdapter.class);
        }
    }

    protected StdioMessageAdapter(StdioMessageAdapter template)
    {
        super(template);
        message = template.message;
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
        String msg = getPayloadAsString();
        return msg.getBytes();
    }

    /**
     * @return the current message
     */
    public Object getPayload()
    {
        return message;
    }

    public ThreadSafeAccess newThreadCopy()
    {
        return new StdioMessageAdapter(this);
}

}
