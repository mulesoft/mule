/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */

package org.mule.providers.http;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessageException;
import org.mule.umo.provider.MessageTypeNotSupportedException;

import java.util.Map;

/**
 * <code>HttpMessageAdapter</code> Wraps an incoming Http Request making
 * the payload and heads available a standard message adapter
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class HttpMessageAdapter extends AbstractMessageAdapter
{
    private Object message = null;

    public HttpMessageAdapter(Object message) throws MessageException
    {
        if (message instanceof Object[])
        {
            this.message = ((Object[])message)[0];
            if(((Object[])message).length > 1) {
                properties = (Map)((Object[])message)[1];
            }
        } else if(message instanceof byte[]){
            this.message = message;
        }  else {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.providers.UMOMessageAdapter#getPayload()
     */
    public Object getPayload()
    {
        return message;
    }

    public boolean isBinary()
    {
        return message instanceof byte[];
    }

    /* (non-Javadoc)
     * @see org.mule.umo.providers.UMOMessageAdapter#getPayloadAsBytes()
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        if (isBinary())
        {
            return (byte[]) message;
        } else
        {
            return ((String) message).getBytes();
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.providers.UMOMessageAdapter#getPayloadAsString()
     */
    public String getPayloadAsString() throws Exception
    {
        if (isBinary())
        {
            return new String((byte[]) message);
        } else
        {
            return (String) message;
        }
    }
}
