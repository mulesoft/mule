/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.xmpp;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;

import java.util.Iterator;

/**
 * <code>XmppMessageAdapter</code> wraps an Smack XMPP packet
 * 
 * @author Peter Braswell
 * @version $Revision$
 */
public class XmppMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -4003299444661664762L;

    private Packet message;

    public XmppMessageAdapter(Object message) throws MessagingException
    {
        if (message instanceof Packet)
        {
            this.message = (Packet)message;
            for (Iterator iter = this.message.getPropertyNames(); iter.hasNext();)
            {
                String name = (String)iter.next();
                setProperty(name, this.message.getProperty(name));
            }
            if (this.message instanceof Message)
            {
                setProperty("subject", ((Message)this.message).getSubject());
                setProperty("thread", ((Message)this.message).getThread());
            }
        }
        else
        {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @param encoding The encoding to use when transforming the message (if
     *            necessary). The parameter is used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception
    {
        if (message instanceof Message)
        {
            return ((Message)message).getBody();
        }
        else
        {
            return message.toString();
        }
    }

    public byte[] getPayloadAsBytes() throws Exception
    {
        if (message instanceof Message)
        {
            return ((Message)message).getBody().getBytes();
        }
        else
        {
            return message.toString().getBytes();
        }
    }

    public Object getPayload()
    {
        return message;
    }

    public String getUniqueId()
    {
        return message.getPacketID();
    }
}
