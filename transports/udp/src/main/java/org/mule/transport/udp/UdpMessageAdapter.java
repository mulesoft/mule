/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.udp;

import org.mule.api.ThreadSafeAccess;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.transport.AbstractMessageAdapter;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * <code>UdpMessageAdapter</code>
 */

public class UdpMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -7767141617682012504L;

    public static final String ADDRESS_PROPERTY = "packet.address";
    public static final String PORT_PROPERTY = "packet.port";

    private byte[] message;

    public UdpMessageAdapter(Object message) throws MessageTypeNotSupportedException
    {
        if (message instanceof DatagramPacket)
        {
            DatagramPacket dp = (DatagramPacket) message;
            this.message = new byte[dp.getLength()];
            System.arraycopy(dp.getData(), 0, this.message, 0, dp.getLength());

            InetAddress address = dp.getAddress();
            if (address != null)
            {
                setProperty(ADDRESS_PROPERTY, address);
            }

            setProperty(PORT_PROPERTY, new Integer(dp.getPort()));
        }
        else
        {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    protected UdpMessageAdapter(UdpMessageAdapter template)
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

    public ThreadSafeAccess newThreadCopy()
    {
        return new UdpMessageAdapter(this);
    }

}
