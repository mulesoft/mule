/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.functional.AbstractProviderFunctionalTestCase;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

public class UdpConnectorFunctionalTestCase extends AbstractProviderFunctionalTestCase
{
    private static final String MESSAGE = "Hello";

    DatagramSocket s = null;
    URI serverUri = null;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        serverUri = getInDest().getUri();
    }

    protected void doTearDown() throws Exception
    {
        try
        {
            s.close();
        }
        catch (Exception e)
        {
            // ignore
        }
        super.doTearDown();
    }

    protected void sendTestData(int iterations) throws Exception
    {
        InetAddress inet = InetAddress.getByName(serverUri.getHost());

        s = new DatagramSocket(0);
        s.setSoTimeout(2000);

        for (int sentPackets = 0; sentPackets < iterations; sentPackets++)
        {
            String msg = MESSAGE + sentPackets;
            DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(), inet,
                serverUri.getPort());
            s.send(packet);
        }
    }

    protected void receiveAndTestResults() throws Exception
    {
        URI uri = getOutDest().getUri();
        InetAddress inet = InetAddress.getByName(uri.getHost());
        Set receivedMessages = new HashSet(NUM_MESSAGES_TO_SEND);
        int receivedPackets;

        for (receivedPackets = 0; receivedPackets < NUM_MESSAGES_TO_SEND; receivedPackets++)
        {
            DatagramPacket packet = new DatagramPacket(new byte[32], 32, inet, serverUri.getPort());
            s.receive(packet);
            receivedMessages.add(new UdpMessageAdapter(packet).getPayloadAsString());
        }

        assertEquals(NUM_MESSAGES_TO_SEND, receivedPackets);

        for (int i = 0; i < receivedMessages.size(); i++)
        {
            String message = MESSAGE + i + " Received";
            assertTrue("checking for received message '" + message + "'", receivedMessages.contains(message));
        }
    }

    protected UMOEndpointURI getInDest()
    {
        try
        {
            return new MuleEndpointURI("udp://localhost:60131");
        }
        catch (MalformedEndpointException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        try
        {
            return new MuleEndpointURI("udp://localhost:60132");
        }
        catch (MalformedEndpointException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    public UMOConnector createConnector() throws Exception
    {
        UdpConnector connector = new UdpConnector();
        connector.setName("testUdp");
        connector.getDispatcherThreadingProfile().setDoThreading(false);
        return connector;
    }

}
