/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp.issues;

import org.mule.extras.client.MuleClient;
import org.mule.providers.ftp.AbstractFtpServerTestCase;
import org.mule.providers.ftp.server.NamedPayload;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalStreamingTestComponent;
import org.mule.umo.UMOEventContext;

import java.util.HashMap;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

public class MultiStreamMule1696TestCase extends AbstractFtpServerTestCase
{

    public static final String TEST_MESSAGE_2 = "Another test message";
    private static int PORT = 60197;

    public MultiStreamMule1696TestCase()
    {
        super(PORT);
    }

    protected String getConfigResources()
    {
        return "ftp-streaming-test.xml";
    }

    private EventCallback newCallback(final CountDownLatch latch, final AtomicReference message)
    {
        return new EventCallback()
        {
            public synchronized void eventReceived(UMOEventContext context, Object component)
            {
                try
                {
                    FunctionalStreamingTestComponent ftc = (FunctionalStreamingTestComponent) component;
                    logger.debug("Callback called: " + ftc.getSummary());
                    message.set(ftc.getSummary());
                    latch.countDown();
                }
                catch (Exception e)
                {
                    logger.error(e.getMessage(), e);
                }
            }
        };
    }

    public void testSendAndRequest() throws Exception
    {
        MuleClient client = new MuleClient();

        Object ftc = getPojoServiceForComponent("testComponent");
        assertTrue("FunctionalStreamingTestComponent expected", ftc instanceof FunctionalStreamingTestComponent);

        assertNotNull(ftc);
//        assertEquals(1, ftc.getNumber());

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference message = new AtomicReference();
        EventCallback callback = newCallback(latch, message);
        ((FunctionalStreamingTestComponent) ftc).setEventCallback(callback, TEST_MESSAGE.length());

        // send out to FTP server via streaming model
        client.dispatch("tcp://localhost:60196", TEST_MESSAGE, new HashMap());
        NamedPayload payload = awaitUpload();
        assertNotNull(payload);
        logger.info("received message: " + payload);
        assertEquals(TEST_MESSAGE, new String(payload.getPayload()));

        // poll and pull back through test component
        latch.await(getTimeout(), TimeUnit.MILLISECONDS);
        assertEquals("Received stream; length: 16; 'Test...sage'", message.get());

        // repeat, but restart server due to simple state, connection limitations
        stopServer();
        synchronized(this)
        {
            wait(2000); // TCP socket timeout
        }
        startServer();

        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicReference message2 = new AtomicReference();
        EventCallback callback2 = newCallback(latch2, message2);
        ((FunctionalStreamingTestComponent) ftc).setEventCallback(callback2, TEST_MESSAGE_2.length());

        client.dispatch("tcp://localhost:60196", TEST_MESSAGE_2, new HashMap());
        NamedPayload payload2 = awaitUpload();
        assertNotNull(payload2);
        logger.info("received message: " + payload2);
        assertEquals(TEST_MESSAGE_2, new String(payload2.getPayload()));

        latch2.await(getTimeout(), TimeUnit.MILLISECONDS);
        assertEquals("Received stream; length: 20; 'Anot...sage'", message2.get());
    }

}
