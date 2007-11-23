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

import org.mule.extras.client.MuleClient;
import org.mule.impl.ResponseOutputStream;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.URI;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

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
        return "ssl-connector-functional-test.xml";
    }

    protected URI getUri() throws UMOException
    {
        return managementContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("in")
            .getEndpointURI()
            .getUri();
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


    public void xtestDispatchAndReply() throws Exception
    {

        final CountDownLatch callbackCount = new CountDownLatch(1);
        // these don't need to be atomic, they are just used as references
        // so that wecan move the assertions out of the callback (failing
        // in the callback hangs the test - it's not the right thread)
        final AtomicBoolean nonNullOutputStream = new AtomicBoolean(false);
        final AtomicReference certificates = new AtomicReference();

        FunctionalTestComponent ftc = lookupTestComponent("main", "testComponent");
        ftc.setEventCallback(new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object component) throws Exception
            {
                try
                {
                    String result = FunctionalTestComponent.received(context.getMessageAsString());
                    nonNullOutputStream.set(null != context.getOutputStream());
                    if (nonNullOutputStream.get())
                    {
                        logger.debug("reading certificate");
                        certificates.set(context.getMessage().getProperty(SslConnector.LOCAL_CERTIFICATES));
                        if (!((ResponseOutputStream) context.getOutputStream()).getSocket().isClosed())
                        {
                            context.getOutputStream().write(result.getBytes());
                            context.getOutputStream().flush();
                        }
                    }
                }
                finally
                {
                    callbackCount.countDown();
                }
            }
        });

        MuleClient client = new MuleClient();
        UMOMessage response = client.send(getUri().toString(), TEST_MESSAGE, null);
        callbackCount.await(3000999, TimeUnit.MILLISECONDS);
        assertEquals(0, callbackCount.getCount());
        assertEquals(TEST_MESSAGE_RESPONSE, response.getPayloadAsString());
        assertTrue("no output stream", nonNullOutputStream.get());
        assertNotNull("no certificates", certificates.get());
    }

}
