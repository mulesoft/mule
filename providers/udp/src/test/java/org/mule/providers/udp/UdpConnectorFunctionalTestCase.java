/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.udp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.functional.AbstractProviderFunctionalTestCase;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class UdpConnectorFunctionalTestCase extends AbstractProviderFunctionalTestCase
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(UdpConnectorFunctionalTestCase.class);

    DatagramSocket s = null;
    URI serverUri = null;

    protected void doSetUp() throws Exception
    {
        serverUri = getInDest().getUri();

    }

    protected void doTearDown() throws Exception
    {
        try {
            s.close();
        } catch (Exception e) {
        }
        super.tearDown();
    }

    protected void sendTestData(int iterations) throws Exception
    {

        InetAddress inet = InetAddress.getByName(serverUri.getHost());
        s = new DatagramSocket(0);
        for (int i = 0; i < iterations; i++) {
            String msg = "Hello" + i;

            DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(), inet, serverUri.getPort());

            System.out.println("sending message: " + i);
            s.send(packet);
        }
    }

    protected void receiveAndTestResults() throws Exception
    {
        int i = 0;
        URI uri = getOutDest().getUri();
        InetAddress inet = InetAddress.getByName(uri.getHost());
        s.setSoTimeout(2000);
        for (i = 0; i < 100; i++) {

            DatagramPacket packet = new DatagramPacket(new byte[32], 32, inet, serverUri.getPort());

            s.receive(packet);
            UdpMessageAdapter adapter = new UdpMessageAdapter(packet);
            System.out.println("Received message: " + adapter.getPayloadAsString());

        }
        assertEquals(100, i);
        Thread.sleep(3000);
    }

    protected UMOEndpointURI getInDest()
    {
        try {
            return new MuleEndpointURI("udp://localhost:60131");
        } catch (MalformedEndpointException e) {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        try {
            return new MuleEndpointURI("udp://localhost:60132");
        } catch (MalformedEndpointException e) {
            fail(e.getMessage());
            return null;
        }
    }

    public UMOConnector createConnector() throws Exception
    {
        UdpConnector connector = new UdpConnector();
        connector.setName("testUdp");
        connector.getDispatcherThreadingProfile().setDoThreading(false);

        connector.setBufferSize(1024);
        return connector;
    }
}
