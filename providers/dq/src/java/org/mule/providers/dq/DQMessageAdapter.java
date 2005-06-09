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
package org.mule.providers.dq;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.util.Utility;

/**
 * <code>DQMessageAdapter</code> provides a wrapper for a DataQueue Message.
 * Users can obtain the contents of the message through the payload property.
 * 
 * @author m999svm
 */
public class DQMessageAdapter extends AbstractMessageAdapter
{

    private DQMessage message;

    /**
     * Constructor
     * 
     * @param message The message
     */
    public DQMessageAdapter(Object message) throws MessageTypeNotSupportedException
    {
        if (message instanceof DQMessage) {
            this.message = (DQMessage) message;
        } else {
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
        return Utility.objectToByteArray(message);
    }

    /**
     * @see org.mule.umo.provider.UMOMessageAdapter#getPayloadAsString()
     */
    public final String getPayloadAsString() throws Exception
    {
        return new String(getPayloadAsBytes());
    }

}
