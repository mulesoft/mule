/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.udp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.transport.AbstractMuleMessageFactory;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class UdpMuleMessageFactory extends AbstractMuleMessageFactory
{
    public UdpMuleMessageFactory(MuleContext context)
    {
        super(context);
    }

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class[] { DatagramPacket.class };
    }

    @Override
    protected Object extractPayload(Object transportMessage, String encoding) throws Exception
    {
        DatagramPacket packet = (DatagramPacket) transportMessage;
        
        int length = packet.getLength();
        byte[] payload = new byte[length];
        System.arraycopy(packet.getData(), 0, payload, 0, length);

        return payload;
    }

    @Override
    protected void addProperties(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        super.addProperties(message, transportMessage);

        DatagramPacket packet = (DatagramPacket) transportMessage;
        
        InetAddress address = packet.getAddress();
        if (address != null)
        {
            message.setOutboundProperty(UdpConnector.ADDRESS_PROPERTY, address);
        }

        message.setOutboundProperty(UdpConnector.PORT_PROPERTY, Integer.valueOf(packet.getPort()));
    }
}
