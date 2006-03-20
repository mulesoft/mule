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

    

    public byte[] getPayloadAsBytes() throws Exception {
		return convertToBytes(getPayload());
	}



	/**
     * Converts the message implementation into a String representation
     *
     * @param encoding The encoding to use when transforming the message (if necessary). The parameter is
     *                 used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception {
        return message.toString();
    }

    public Object getPayload()
    {
        return message;
    }
}
