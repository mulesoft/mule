/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.multicast;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.udp.UdpMessageAdapter;
import org.mule.tck.functional.AbstractProviderFunctionalTestCase;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URI;

public class MulticastConnectorFunctionalTestCase extends AbstractProviderFunctionalTestCase
{
    private MulticastSocket s1 = null;
    private MulticastSocket s2 = null;
    private InetAddress inet = null;
    private URI uri;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        uri = getInDest().getUri();
        inet = InetAddress.getByName(uri.getHost());
    }

    protected void doTearDown() throws Exception
    {
        try
        {
            s1.close();
        }
        catch (Exception e)
        {
            // ignore
        }
        try
        {
            s2.close();
        }
        catch (Exception e)
        {
            // ignore
        }

        super.doTearDown();
    }

    protected void sendTestData(int iterations) throws Exception
    {

        s1 = new MulticastSocket(uri.getPort());
        s1.joinGroup(inet);

        s2 = new MulticastSocket(uri.getPort());
        s2.joinGroup(inet);

        for (int i = 0; i < iterations; i++)
        {
            String msg = "Hello" + i;

            DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(), inet, uri.getPort());

            System.out.println("sending message: " + i);
            s1.send(packet);
        }
    }

    protected void receiveAndTestResults() throws Exception
    {
        s2.setSoTimeout(2000);
        for (int i = 0; i < 100; i++)
        {

            DatagramPacket packet = new DatagramPacket(new byte[32], 32, inet, uri.getPort());

            s2.receive(packet);
            UdpMessageAdapter adapter = new UdpMessageAdapter(packet);
            System.out.println("Received message: " + adapter.getPayloadAsString());

        }
        Thread.sleep(3000);
    }

    protected UMOEndpointURI getInDest()
    {
        try
        {
            return new MuleEndpointURI("multicast://228.8.9.10:6677");
        }
        catch (MalformedEndpointException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        return getInDest();
    }

    public UMOConnector createConnector() throws Exception
    {
        MulticastConnector connector = new MulticastConnector();
        connector.setName("testMulticast");
        connector.getDispatcherThreadingProfile().setDoThreading(false);

        connector.setBufferSize(1024);
        return connector;
    }

}
