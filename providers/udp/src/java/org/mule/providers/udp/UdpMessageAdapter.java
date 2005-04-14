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
package org.mule.providers.udp;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
/**
 * <code>UdpMessageAdapter</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class UdpMessageAdapter extends AbstractMessageAdapter
{
    public static final String ADDRESS_PROPERTY = "packet.address";
    public static final String PORT_PROPERTY = "packet.port";

    private byte[] message;

    public UdpMessageAdapter(Object message) throws MessagingException {
        if(message instanceof DatagramPacket) {
            DatagramPacket dp = (DatagramPacket)message;
            this.message = new byte[dp.getLength()];

            ByteArrayInputStream bais = new ByteArrayInputStream(dp.getData());
            bais.read(this.message, 0, dp.getLength());
            try
            {
                bais.close();
            } catch (IOException e) { }

            setProperty(ADDRESS_PROPERTY, dp.getAddress());
            setProperty(PORT_PROPERTY, new Integer(dp.getPort()));

        } else{
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    public String getPayloadAsString() throws Exception
    {
        return new String(message);
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
