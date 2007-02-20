/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.RegistryContext;
import org.mule.impl.ResponseOutputStream;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.functional.AbstractProviderFunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;

public class TcpConnectorFunctionalTestCase extends AbstractProviderFunctionalTestCase
{
    private Socket s;
    private int port = 61655;

    protected void doTearDown() throws Exception
    {
        if (s != null)
        {
            s.close();
        }
    }

    protected void sendTestData(int iterations) throws Exception
    {
        RegistryContext.getConfiguration().setDefaultSynchronousEndpoints(false);
        URI uri = getInDest().getUri();
        for (int i = 0; i < iterations; i++)
        {
            s = createSocket(uri);
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
            dos.write("Hello".getBytes());
            dos.flush();
            logger.info("Sent message: " + i);
            dos.close();
        }
    }

    protected Socket createSocket(URI uri) throws IOException
    {
        return new Socket(uri.getHost(), uri.getPort());
    }

    protected void receiveAndTestResults() throws Exception
    {
        Thread.sleep(3000);
        assertEquals(100, callbackCount);
    }

    protected UMOEndpointURI getInDest()
    {
        try
        {
            return new MuleEndpointURI("tcp://localhost:" + port);
        }
        catch (EndpointException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        return null;
    }

    public UMOConnector createConnector() throws Exception
    {
        TcpConnector connector = new TcpConnector();
        connector.setName("testTcp");
        connector.getDispatcherThreadingProfile().setDoThreading(false);

        return connector;
    }

    public void testDispatchAndReply() throws Exception
    {
        RegistryContext.getConfiguration().setDefaultSynchronousEndpoints(false);
        descriptor = getTestDescriptor("testComponent", FunctionalTestComponent.class.getName());

        initialiseComponent(descriptor, new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
            {
                callbackCount++;
                String result = "Received Async event: " + context.getMessageAsString();
                assertNotNull(context.getOutputStream());

                if (!((ResponseOutputStream)context.getOutputStream()).getSocket().isClosed())
                {
                    context.getOutputStream().write(result.getBytes());
                    context.getOutputStream().flush();
                }

                callbackCalled = true;
            }
        });
        // Start the server
        managementContext.start();

        URI uri = getInDest().getUri();
        s = createSocket(uri);
        DataOutputStream dos = new DataOutputStream((s.getOutputStream()));
        dos.write("Hello".getBytes());
        dos.flush();

        afterInitialise();

        DataInputStream dis = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        byte[] buf = new byte[32];
        int x = dis.read(buf);
        assertTrue(x > -1);
        assertTrue(new String(buf, 0, x).startsWith("Received Async event"));
        assertEquals(1, callbackCount);

    }
}
