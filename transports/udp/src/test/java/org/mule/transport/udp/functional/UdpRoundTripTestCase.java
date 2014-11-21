/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.udp.functional;

import static org.junit.Assert.assertEquals;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.NetworkUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class UdpRoundTripTestCase extends AbstractServiceAndFlowTestCase
{

    @Rule
    public DynamicPort outPort = new DynamicPort("outPort");

    @Rule
    public DynamicPort inPort = new DynamicPort("inPort");

    public UdpRoundTripTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "udp-roundtrip-test-config-service.xml"},
            {ConfigVariant.FLOW, "udp-roundtrip-test-config-flow.xml"}
        });
    }

    @Test
    public void testSendAndReceiveUDP() throws IOException
    {
        // the socket we talk to
        DatagramSocket socket = new DatagramSocket(inPort.getNumber(), NetworkUtils.getLocalHost());

        // prepare outgoing packet
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(bytesOut);
        dataOut.writeFloat(1.0f);
        dataOut.writeFloat(2.0f);
        byte[] bytesToSend = bytesOut.toByteArray();

        DatagramPacket outboundPacket = new DatagramPacket(bytesToSend, bytesToSend.length,
            NetworkUtils.getLocalHost(), outPort.getNumber());
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
