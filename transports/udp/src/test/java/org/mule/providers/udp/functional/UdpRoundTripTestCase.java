/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.udp.functional;

import org.mule.tck.FunctionalTestCase;

import edu.emory.mathcs.backport.java.util.Arrays;

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

    protected String getConfigResources()
    {
        return "udp-roundtrip-test-config.xml";
    }

    public void testSendAndReceiveUDP() throws IOException
    {
        int outPort = 9001;
        int inPort = 9002;

        // the socket we talk to
        DatagramSocket socket = new DatagramSocket(inPort, InetAddress.getLocalHost());

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
        assertEquals(Arrays.toString(outboundPacket.getData()), Arrays.toString(inboundPacket.getData()));

        // make sure the contents are really the same
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(inboundPacket.getData());
        DataInputStream dataIn = new DataInputStream(bytesIn);
        // the delta is only here to make JUnit happy
        assertEquals(1.0f, dataIn.readFloat(), 0.1f);
        assertEquals(2.0f, dataIn.readFloat(), 0.1f);
    }

}
