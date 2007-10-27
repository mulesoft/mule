/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.integration;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalStreamingTestComponent;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEventContext;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

import java.util.HashMap;

/**
 * This test is more about testing the streaming model than the TCP provider, really.
 */
public class StreamingTestCase extends FunctionalTestCase
{

    public static final int TIMEOUT = 300000;
    public static final String TEST_MESSAGE = "Test TCP Request";
    public static final String RESULT = "Received stream; length: 16; 'Test...uest'";


    public StreamingTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "tcp-streaming-test.xml";
    }

    public void testSend() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference message = new AtomicReference();
        final AtomicInteger loopCount = new AtomicInteger(0);

        EventCallback callback = new EventCallback()
        {
            public synchronized void eventReceived(UMOEventContext context, Object component)
            {
                try
                {
                    logger.info("called " + loopCount.incrementAndGet() + " times");
                    FunctionalStreamingTestComponent ftc = (FunctionalStreamingTestComponent) component;
                    // without this we may have problems with the many repeats
                    if (1 == latch.getCount())
                    {
                        message.set(ftc.getSummary());
                        assertEquals(RESULT, message.get());
                        latch.countDown();
                    }
                }
                catch (Exception e)
                {
                    logger.error(e.getMessage(), e);
                }
            }
        };

        MuleClient client = new MuleClient();

        // this works only if singleton set in descriptor
        FunctionalStreamingTestComponent ftc = (FunctionalStreamingTestComponent)
            lookupComponent("echoModel", "testComponent");
        assertNotNull(ftc);

        ftc.setEventCallback(callback, TEST_MESSAGE.length());

        client.dispatch("tcp://localhost:65432", TEST_MESSAGE, new HashMap());

        latch.await(10, TimeUnit.SECONDS);
        assertEquals(RESULT, message.get());
    }


}
