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
package org.mule.providers.xmpp;

import org.jivesoftware.smack.packet.Message;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.umo.provider.UniqueIdNotSupportedException;

import java.util.Iterator;

/**
 * <code>XmppMessageAdapter</code> TODO
 *
 * @author Peter Braswell
 * @version $Revision$
 */
public class XmppMessageAdapter extends AbstractMessageAdapter
{
    private Message message;

    public XmppMessageAdapter(Object message) throws MessagingException
    {
        if (message instanceof Message)
        {
            this.message = (Message) message;
            for (Iterator iter = this.message.getPropertyNames(); iter.hasNext();) {
                String name =  (String)iter.next();
                setProperty(name, this.message.getProperty(name));
            }
            setProperty("subject", this.message.getSubject());
            setProperty("thread", this.message.getThread());
        } else
        {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    public String getPayloadAsString() throws Exception
    {
        return message.getBody();
    }

    public byte[] getPayloadAsBytes() throws Exception
    {
        return message.getBody().getBytes();
    }

    public Object getPayload()
    {
        return message;
    }

    public String getUniqueId() throws UniqueIdNotSupportedException {
        return message.getPacketID();
    }
}
