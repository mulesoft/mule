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

import org.mule.api.MuleException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.transport.AbstractMessageAdapter;
import org.mule.transport.MessageAdapterSerialization;
import org.mule.util.StringUtils;

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

    private final Packet packet;

    public XmppMessageAdapter(Object payload) throws MuleException
    {
        if (payload instanceof Packet)
        {
            packet = (Packet) payload;

            for (String name : packet.getPropertyNames())
            {
                this.setProperty(name, packet.getProperty(name));
            }

            if (packet instanceof Message)
            {
                Message msg = (Message) packet;
                
                this.setProperty("subject", StringUtils.defaultIfEmpty(msg.getSubject(), DEFAULT_SUBJECT));
                this.setProperty("thread", StringUtils.defaultIfEmpty(msg.getThread(), DEFAULT_THREAD));
            }
        }
        else
        {
            throw new MessageTypeNotSupportedException(payload, getClass());
        }
    }

    protected XmppMessageAdapter(XmppMessageAdapter template)
    {
        super(template);
        packet = template.packet;
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
        if (packet instanceof Message)
        {
            return ((Message)packet).getBody();
        }
        else
        {
            return packet.toString();
        }
    }

    public byte[] getPayloadAsBytes() throws Exception
    {
        if (packet instanceof Message)
        {
            return ((Message)packet).getBody().getBytes();
        }
        else
        {
            return packet.toString().getBytes();
        }
    }

    public Object getPayload()
    {
        return packet;
    }

    @Override
    public String getUniqueId()
    {
        return packet.getPacketID();
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
