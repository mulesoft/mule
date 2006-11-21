/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.rmi;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.provider.MessageTypeNotSupportedException;

/**
 * Wraps an object obtained by calling a method on a Remote object
 */

public class RmiMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1765089871661318129L;

    private final Object message;

    public RmiMessageAdapter(Object message) throws MessageTypeNotSupportedException
    {
        if (message == null)
        {
            throw new MessageTypeNotSupportedException(null, getClass());
        }
        this.message = message;
    }

    public byte[] getPayloadAsBytes() throws Exception
    {
        return convertToBytes(getPayload());
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

    public Object getPayload()
    {
        return message;
    }
}
