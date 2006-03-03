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
 *
 */
package org.mule.providers.ftp;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class FtpMessageAdapter extends AbstractMessageAdapter
{

    private byte[] message;

    public FtpMessageAdapter(Object message) throws MessagingException
    {
        if (message instanceof byte[]) {
            this.message = (byte[]) message;
        }
        else {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
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
        return new String(message, encoding);
    }

    public byte[] getPayloadAsBytes() throws Exception
    {
        return message;
    }

    public Object getPayload()
    {
        return message;
    }

}
