/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.dq;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.provider.MessageTypeNotSupportedException;

/**
 * <code>DQMessageAdapter</code> provides a wrapper for a DataQueue Message. Users
 * can obtain the contents of the message through the payload property.
 */
public class DQMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -7484858345063740661L;

    private static final SerializableToByteArray serializableToByteArray = new SerializableToByteArray();

    private final DQMessage message;

    /**
     * Constructor
     * 
     * @param message The message
     */
    public DQMessageAdapter(Object message) throws MessageTypeNotSupportedException
    {
        if (message instanceof DQMessage)
        {
            this.message = (DQMessage)message;
        }
        else
        {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    /**
     * @see org.mule.umo.provider.UMOMessageAdapter#getPayload()
     */
    public final Object getPayload()
    {
        return message;
    }

    /**
     * @see org.mule.umo.provider.UMOMessageAdapter#getPayloadAsBytes()
     */

    public final byte[] getPayloadAsBytes() throws Exception
    {
        return (byte[])serializableToByteArray.doTransform(message, getEncoding());
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
        return new String(getPayloadAsBytes(), encoding);
    }

}
