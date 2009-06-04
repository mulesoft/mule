/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.xmpp;

import org.mule.api.MessagingException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.transport.AbstractMessageAdapter;
import org.mule.transport.MessageAdapterSerialization;
import org.mule.util.StringUtils;

import java.util.Iterator;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/**
 * <code>XmppMessageAdapter</code> wraps a Smack XMPP packet
 */
public class XmppMessageAdapter extends AbstractMessageAdapter implements MessageAdapterSerialization
{
    public static final String DEFAULT_SUBJECT = "(no subject)";
    public static final String DEFAULT_THREAD = "(no thread)";

    /**
     * Serial version
     */
    private static final long serialVersionUID = -4003299444661664762L;

    private final Packet message;

    public XmppMessageAdapter(Object message) throws MessagingException
    {
        if (message instanceof Packet)
        {
            this.message = (Packet) message;

            for (Iterator iter = this.message.getPropertyNames(); iter.hasNext();)
            {
                String name = (String)iter.next();
                this.setProperty(name, this.message.getProperty(name));
            }

            if (this.message instanceof Message)
            {
                Message msg = (Message) this.message;
                
                this.setProperty("subject", StringUtils.defaultIfEmpty(msg.getSubject(), DEFAULT_SUBJECT));
                this.setProperty("thread", StringUtils.defaultIfEmpty(msg.getThread(), DEFAULT_THREAD));
            }
        }
        else
        {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    protected XmppMessageAdapter(XmppMessageAdapter template)
    {
        super(template);
        message = template.message;
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

    @Override
    public String getUniqueId()
    {
        return message.getPacketID();
    }

    @Override
    public ThreadSafeAccess newThreadCopy()
    {
        return new XmppMessageAdapter(this);
    }

    public byte[] getPayloadForSerialization() throws Exception
    {
        return this.getPayloadAsBytes();
    }

}
