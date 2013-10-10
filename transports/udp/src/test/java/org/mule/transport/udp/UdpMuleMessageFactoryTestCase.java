/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.udp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

public class UdpMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    private static final int PORT = 4242;

    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new UdpMuleMessageFactory(muleContext);
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        InetAddress address = InetAddress.getLocalHost();
        return new DatagramPacket(TEST_MESSAGE.getBytes(), TEST_MESSAGE.length(), address, PORT);
    }

    @Override
    protected Object getUnsupportedTransportMessage()
    {
        return "this is an invalid payload for UdpMuleMessageFactory";
    }

    @Override
    public void testValidPayload() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();

        MuleMessage message = factory.create(getValidTransportMessage(), encoding);
        assertNotNull(message);
        assertPayload(message);
        assertEquals(PORT, message.getInboundProperty(UdpConnector.PORT_PROPERTY));
        assertNotNull(message.getInboundProperty(UdpConnector.ADDRESS_PROPERTY));
    }

    private void assertPayload(MuleMessage message)
    {
        byte[] expected = TEST_MESSAGE.getBytes();
        byte[] result = (byte[]) message.getPayload();
        assertTrue(Arrays.equals(expected, result));
    }
}
