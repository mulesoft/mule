/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ssl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.URI;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.mule.MuleManager;
import org.mule.impl.ResponseOutputStream;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.functional.AbstractProviderFunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class SslConnectorFunctionalTestCase extends AbstractProviderFunctionalTestCase
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(SslConnectorFunctionalTestCase.class);

    private int port = 61655;
    private Socket s;

    protected UMOEndpointURI getInDest()
    {
        try
        {
            return new MuleEndpointURI("ssl://localhost:" + port);
        }
        catch (MalformedEndpointException e)
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
        SslConnector cnn = SslConnectorTestCase.createConnector(false);
        return cnn;
    }

    protected Socket createSocket(URI uri) throws Exception
    {
        SslConnector conn = (SslConnector)connector;
        SSLContext context;
        context = SSLContext.getInstance(conn.getProtocol());
        context.init(conn.getKeyManagerFactory().getKeyManagers(), conn.getTrustManagerFactory()
            .getTrustManagers(), null);
        SSLSocketFactory factory = context.getSocketFactory();
        Socket socket = factory.createSocket(uri.getHost(), uri.getPort());

        // this will force open the socket and start SSL/TLS negotiation
        // sslSocket.startHandshake();

        return socket;
    }

    protected void doTearDown() throws Exception
    {
        if (s != null)
        {
            s.close();
            s = null;
        }
    }

    protected void sendTestData(int iterations) throws Exception
    {
        MuleManager.getConfiguration().setSynchronous(false);
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

    protected void receiveAndTestResults() throws Exception
    {
        Thread.sleep(3000);
        assertEquals(100, callbackCount);

    }

    public void testDispatchAndReply() throws Exception
    {
        MuleManager.getConfiguration().setSynchronous(false);
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
        MuleManager.getInstance().start();

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
