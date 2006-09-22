/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.udp;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;

import java.net.DatagramPacket;

/**
 * <code>UdpMessageAdapter</code>
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
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

    public UdpMessageAdapter(Object message) throws MessagingException
    {
        if (message instanceof DatagramPacket) {
            DatagramPacket dp = (DatagramPacket)message;
            this.message = new byte[dp.getLength()];
            System.arraycopy(dp.getData(), 0, this.message, 0, dp.getLength());
            setProperty(ADDRESS_PROPERTY, dp.getAddress());
            setProperty(PORT_PROPERTY, new Integer(dp.getPort()));
        }
        else {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    /**
     * Converts the message implementation into a String representation
     *
     * @param encoding The encoding to use when transforming the message (if necessary). The parameter is
     *                 used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception {
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
}
