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
package org.mule.providers;

import org.mule.umo.provider.UniqueIdNotSupportedException;
import org.mule.util.UUID;

/**
 * <code>DefaultMessageAdapter</code> can be used to wrap an arbitary object
 * where no special 'apapting' is needed. The adpapter allows for a set of
 * properties to be associated with an object.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class DefaultMessageAdapter extends AbstractMessageAdapter
{
    private Object message;
    private String id = null;

    public DefaultMessageAdapter(Object message)
    {
        id = new UUID().getUUID();
        if (message == null) {
            this.message = new NullPayload();
        } else {
            this.message = message;
        }
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString() throws Exception
    {
        if (message instanceof byte[]) {
            return new String((byte[]) message);
        } else {
            return message.toString();
        }
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @return String representation of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return getPayloadAsString().getBytes();
    }

    /**
     * @return the current message
     */
    public Object getPayload()
    {
        return message;
    }

    public String getUniqueId() throws UniqueIdNotSupportedException
    {
        return id;
    }
}
