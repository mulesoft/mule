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
package org.mule.providers.rmi;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.util.Utility;

/**
 * <code>RmiMessageAdapter</code> TODO
 * 
 * @author <a href="mailto:fsweng@bass.com.my">fs Weng</a>
 * @version $Revision$
 */

public class RmiMessageAdapter extends AbstractMessageAdapter
{
    private Object message;

    public RmiMessageAdapter(Object message) throws MessageTypeNotSupportedException
    {
        if (message == null) {
            throw new MessageTypeNotSupportedException(null, getClass());
        }
        this.message = message;
    }

    public String getPayloadAsString() throws Exception
    {
        return message.toString();
    }

    public byte[] getPayloadAsBytes() throws Exception
    {
        return Utility.objectToByteArray(message);
    }

    public Object getPayload()
    {
        return message;
    }
}
