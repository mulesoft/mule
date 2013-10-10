/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.udp;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        assertEquals(PORT, message.getOutboundProperty(UdpConnector.PORT_PROPERTY));
        assertNotNull(message.getOutboundProperty(UdpConnector.ADDRESS_PROPERTY));
    }
    
    private void assertPayload(MuleMessage message)
    {
        byte[] expected = TEST_MESSAGE.getBytes();
        byte[] result = (byte[]) message.getPayload();
        assertTrue(Arrays.equals(expected, result));
    }
}
