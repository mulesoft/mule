/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.udp.functional;

import org.mule.tck.DynamicPortTestCase;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpResponseTransformerTestCase extends DynamicPortTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "udp-response-transformer-config.xml";
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }

    public void testResponseTransformer() throws Exception
    {
        DatagramSocket socket = null;

        try
        {
            socket = new DatagramSocket();
            socket.setSoTimeout(RECEIVE_TIMEOUT);

            byte[] buf = TEST_MESSAGE.getBytes();
            int port = getPorts().get(0).intValue();
            DatagramPacket packet = new DatagramPacket(buf, buf.length,
                    InetAddress.getByName("localhost"), port);

            socket.send(packet);

            packet = new DatagramPacket(new byte[128], 128);
            socket.receive(packet);

            String expected = TEST_MESSAGE + " In Out Out2";
            String result = new String(packet.getData()).trim();
            assertEquals(expected, result);
        }
        finally
        {
            if (socket != null)
            {
                socket.close();
            }
        }
    }
}


