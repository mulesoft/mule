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
 */
package org.mule.providers.xmpp;

import org.jivesoftware.smack.packet.Message;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessageException;
import org.mule.umo.provider.MessageTypeNotSupportedException;

/**
 * <code>XmppMessageAdapter</code> TODO
 *
 * @author Peter Braswell
 * @version $Revision$
 */
public class XmppMessageAdapter extends AbstractMessageAdapter
{
    private Message message;

    public XmppMessageAdapter(Object message) throws MessageException
    {
        if (message instanceof Message)
        {
            this.message = (Message) message;
        } else
        {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    public String getPayloadAsString() throws Exception
    {
        return message.toString();
    }

    public byte[] getPayloadAsBytes() throws Exception
    {
        return message.toString().getBytes();
    }

    public Object getPayload()
    {
        return message;
    }
}
