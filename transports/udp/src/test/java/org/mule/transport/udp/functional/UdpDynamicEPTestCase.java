/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.udp.functional;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.ConfigurableKeyedObjectPool;

public class UdpDynamicEPTestCase extends AbstractServiceAndFlowTestCase
{
    public UdpDynamicEPTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "udp-roundtrip-dynamicep-test-config-service.xml"},
            {ConfigVariant.FLOW, "udp-roundtrip-dynamicep-test-config-flow.xml"}
        });
    }

    @Test
    public void testSendAndReceiveUDP() throws IOException
    {
        int outPort = 61000;
        DatagramSocket socket = null;
        try
        {
            socket = new DatagramSocket();

            // prepare outgoing packet
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(bytesOut);
            dataOut.writeFloat(1.0f);
            dataOut.writeFloat(2.0f);
            byte[] bytesToSend = bytesOut.toByteArray();

            DatagramPacket outboundPacket = new DatagramPacket(bytesToSend, bytesToSend.length,
                InetAddress.getLocalHost(), outPort);
            socket.send(outboundPacket);

            // receive whatever came back
            byte[] receiveBuffer = new byte[bytesToSend.length];
            DatagramPacket inboundPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(inboundPacket);

            // compare byte buffers as strings so we get to see the diff
            assertEquals(Arrays.toString(outboundPacket.getData()),
                Arrays.toString(inboundPacket.getData()));

            // make sure the contents are really the same
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(inboundPacket.getData());
            DataInputStream dataIn = new DataInputStream(bytesIn);
            // the delta is only here to make JUnit happy
            assertEquals(1.0f, dataIn.readFloat(), 0.1f);
            assertEquals(2.0f, dataIn.readFloat(), 0.1f);
        }
        finally
        {
            try
            {
                if (socket != null)
                {
                    socket.close();
                }
                socket = null;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        CustomUdpConnector udpConnector = (CustomUdpConnector) muleContext.getRegistry().lookupConnector("connector.udp.0");
        ConfigurableKeyedObjectPool pool = udpConnector.getDispatchers();
        assertEquals(0, pool.getNumActive());
    }
}
