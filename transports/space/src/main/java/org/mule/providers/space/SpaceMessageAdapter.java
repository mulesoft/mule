/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.space;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.util.UUID;

/**
 * Wraps a JavaSpaces Entry object
 */
public class SpaceMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -2984429022763795361L;

    private final String id;
    private final Object message;

    /**
     * Creates a default message adapter with properties and attachments
     * 
     * @param message the message to wrap. If this is null and NullPayload object
     *            will be used
     * @see org.mule.providers.NullPayload
     */
    public SpaceMessageAdapter(Object message) throws MessageTypeNotSupportedException
    {
        if (message == null)
        {
            throw new MessageTypeNotSupportedException(null, getClass());
        }
        else
        {
            this.id = UUID.getUUID();
            this.message = message;
        }
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

    /**
     * @return the current message
     */
    public Object getPayload()
    {
        return message;
    }

    public String getUniqueId()
    {
        return id;
    }

}
