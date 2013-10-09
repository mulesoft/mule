/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
        addAddressProperty(message, packet);
        addPortProperty(message, packet);
    }

    private void addAddressProperty(DefaultMuleMessage message, DatagramPacket packet)
    {
        InetAddress address = packet.getAddress();
        if (address != null)
        {
            message.setInboundProperty(UdpConnector.ADDRESS_PROPERTY, address);
        }
    }

    private void addPortProperty(DefaultMuleMessage message, DatagramPacket packet)
    {
        Integer port = Integer.valueOf(packet.getPort());
        message.setInboundProperty(UdpConnector.PORT_PROPERTY, port);
    }
}
