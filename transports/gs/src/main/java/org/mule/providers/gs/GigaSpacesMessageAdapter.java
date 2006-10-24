/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.gs;

import com.j_spaces.core.client.ExternalEntry;

import net.jini.core.entry.Entry;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.umo.provider.UMOMessageAdapter;

public class GigaSpacesMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -7125632542442640739L;

    private final GigaSpacesEntryConverter converter = new GigaSpacesEntryConverter();

    private final Object message;

    /**
     * Creates a default message adapter with properties and attachments
     * 
     * @param message the message to wrap. If this is null and NullPayload object
     *            will be used
     * @see org.mule.providers.NullPayload
     */
    public GigaSpacesMessageAdapter(Object message) throws MessageTypeNotSupportedException
    {
        if (message == null)
        {
            // TODO this is not what the javadocs say?!
            throw new MessageTypeNotSupportedException(null, getClass());
        }
        else
        {
            this.message = message;
        }
    }

    /**
     * @see UMOMessageAdapter#getPayloadAsBytes()
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return convertToBytes(getPayload());
    }

    /**
     * @see UMOMessageAdapter#getPayloadAsString()
     */
    public String getPayloadAsString(String encoding) throws Exception
    {
        return message.toString();
    }

    /**
     * @see UMOMessageAdapter#getPayload()
     */
    public Object getPayload()
    {
        if (message instanceof ExternalEntry)
        {
            return converter.toPojo((Entry)message);
        }
        return message;
    }

}
