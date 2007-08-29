/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ssl;

import org.mule.impl.ResponseOutputStream;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.UMOEventContext;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.URI;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * Note that this test doesn't test the socket from the connector itself (and so ran
 * with no problems when the connector was not using SSL).  Rather than alter this
 * test case (which I don't completely understand, and which may be useful in other
 * ways) I have added an additional test in {@link org.mule.providers.ssl.SslFunctionalTestCase}
 */
public class SslConnectorFunctionalTestCase extends FunctionalTestCase
{
    private Socket socket;

    protected String getConfigResources()
    {
        return "/ssl-connector-functional-test.xml";
    }

    protected URI getUri()
    {
        return managementContext.getRegistry().lookupEndpoint("in").getEndpointURI().getUri();
    }

    protected Socket createSocket(URI uri) throws Exception
    {
        SslConnector connector = (SslConnector) managementContext.getRegistry().lookupConnector("SslConnector");
        SSLContext context;
        context = SSLContext.getInstance(connector.getProtocol());
        context.init(connector.getKeyManagerFactory().getKeyManagers(), connector.getTrustManagerFactory()
                .getTrustManagers(), null);
        SSLSocketFactory factory = context.getSocketFactory();
        return factory.createSocket(uri.getHost(), uri.getPort());
    }

    protected void doTearDown() throws Exception
    {
        if (socket != null)
        {
            socket.close();
            socket = null;
        }
    }

    protected void sendTestData(int iterations) throws Exception
    {
        URI uri = getUri();
        for (int i = 0; i < iterations; i++)
        {
            socket = createSocket(uri);
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            dos.write(TEST_MESSAGE.getBytes());
            dos.flush();
            logger.info("Sent message: " + i);
            dos.close();
        }
    }

    public void testSend() throws Exception
    {

        final CountDownLatch callbackCount = new CountDownLatch(100);

        FunctionalTestComponent ftc = lookupTestComponent("main", "testComponent");
        ftc.setEventCallback(new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object component)
            {
                callbackCount.countDown();
                assertNull(context.getCurrentTransaction());
            }
        });

        sendTestData(100);

        callbackCount.await(3000, TimeUnit.MILLISECONDS);
        assertEquals(0, callbackCount.getCount());
    }


    public void testDispatchAndReply() throws Exception
    {

        final CountDownLatch callbackCount = new CountDownLatch(1);

        FunctionalTestComponent ftc = lookupTestComponent("main", "testComponent");
        ftc.setEventCallback(new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object component) throws Exception
            {
                callbackCount.countDown();
                String result = FunctionalTestComponent.received(context.getMessageAsString());

                assertNotNull(context.getOutputStream());
                assertNotNull(context.getMessage().getProperty(SslConnector.LOCAL_CERTIFICATES));

                if (!((ResponseOutputStream) context.getOutputStream()).getSocket().isClosed())
                {
                    context.getOutputStream().write(result.getBytes());
                    context.getOutputStream().flush();
                }

            }
        });

        URI uri = getUri();
        socket = createSocket(uri);
        DataOutputStream dos = new DataOutputStream((socket.getOutputStream()));
        dos.write(TEST_MESSAGE.getBytes());
        dos.flush();

        DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        byte[] buf = new byte[32];
        int x = dis.read(buf);
        assertTrue(x > -1);
        assertEquals(new String(buf, 0, x), TEST_MESSAGE_RESPONSE);
        assertEquals(0, callbackCount.getCount());

    }
}
