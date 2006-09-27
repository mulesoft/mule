/*
 * $$Id: $$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.providers.udp;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.tck.FunctionalTestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpRoundTripTestCase extends FunctionalTestCase
{
    private static Log log = LogFactory.getLog(UdpRoundTripTestCase.class);


    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/udp/udp-roundtrip-test-config.xml";
    }

    public void testSendAndReceiveUDP() throws IOException
    {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(bytesOut);
        dataOut.writeFloat(1.0f);
        dataOut.writeFloat(2.0f);

        int outPort = 9001;
        int inPort = 9002;

        DatagramSocket socket = new DatagramSocket(inPort, InetAddress.getLocalHost());

        byte[] bytes = bytesOut.toByteArray();
        DatagramPacket outboundPacket = new DatagramPacket(bytes, bytes.length, InetAddress
                        .getLocalHost(), outPort);
        socket.send(outboundPacket);
        log.info("sent bytes: " + Arrays.toString(outboundPacket.getData()));

        byte[] buffer = new byte[bytes.length];
        DatagramPacket inboundPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(inboundPacket);

        ByteArrayInputStream bytesIn = new ByteArrayInputStream(inboundPacket.getData());
        log.info("recv bytes: " + Arrays.toString(inboundPacket.getData()));

        DataInputStream dataIn = new DataInputStream(bytesIn);
        assertEquals(1.0f, dataIn.readFloat());
        assertEquals(2.0f, dataIn.readFloat());
        //log.info("float #1: " + dataIn.readFloat());
        //log.info("float #2: " + dataIn.readFloat());
    }

}
