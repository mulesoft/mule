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
import org.jivesoftware.smack.packet.Packet;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.umo.provider.UniqueIdNotSupportedException;

import java.util.Iterator;

/**
 * <code>XmppMessageAdapter</code> wraps an Smack XMPP packet
 * 
 * @author Peter Braswell
 * @version $Revision$
 */
public class XmppMessageAdapter extends AbstractMessageAdapter
{
    private Packet message;

    public XmppMessageAdapter(Object message) throws MessagingException
    {
        if (message instanceof Packet) {
            this.message = (Packet) message;
            for (Iterator iter = this.message.getPropertyNames(); iter.hasNext();) {
                String name = (String) iter.next();
                setProperty(name, this.message.getProperty(name));
            }
            if(this.message instanceof Message) {
                setProperty("subject", ((Message)this.message).getSubject());
                setProperty("thread", ((Message)this.message).getThread());
            }
        } else {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    public String getPayloadAsString() throws Exception
    {
        if(message instanceof Message) {
            return ((Message)message).getBody();
        } else {
            return message.toString();
        }
    }

    public byte[] getPayloadAsBytes() throws Exception
    {
        if(message instanceof Message) {
            return ((Message)message).getBody().getBytes();
        } else {
            return message.toString().getBytes();
        }
    }

    public Object getPayload()
    {
        return message;
    }

    public String getUniqueId() throws UniqueIdNotSupportedException
    {
        return message.getPacketID();
    }
}
