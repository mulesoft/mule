/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp;

import org.mule.api.MuleEventContext;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalStreamingTestComponent;

import java.util.HashMap;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

/**
 * We don't have an integrated ftp server (yet), and synchronous return doesn't work
 * with streaming, as far as i can tell, so the best we can do here is dispatch a
 * through a streaming bridge to the test server, then pull it back again (again,
 * through the streaming model).
 */
public class FtpStreamingTestCase extends AbstractFtpServerTestCase
{

    private static int PORT = 60197;

    public FtpStreamingTestCase()
    {
        super(PORT);
    }

    protected String getConfigResources()
    {
        return "ftp-streaming-test.xml";
    }

    public void testSendAndRequest() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference message = new AtomicReference();
        final AtomicInteger loopCount = new AtomicInteger(0);

        EventCallback callback = new EventCallback()
        {
            public synchronized void eventReceived(MuleEventContext context, Object component)
            {
                try
                {
                    logger.info("called " + loopCount.incrementAndGet() + " times");
                    FunctionalStreamingTestComponent ftc = (FunctionalStreamingTestComponent) component;
                    // without this we may have problems with the many repeats
                    if (1 == latch.getCount())
                    {
                        message.set(ftc.getSummary());
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

        Object component = getComponent("testComponent");
        assertTrue("FunctionalStreamingTestComponent expected", 
            component instanceof FunctionalStreamingTestComponent);
        FunctionalStreamingTestComponent ftc = (FunctionalStreamingTestComponent) component;
        assertNotNull(ftc);
        //assertEquals(1, ftc.getNumber());

        ftc.setEventCallback(callback, TEST_MESSAGE.length());
       
        // send out to FTP server via streaming model
        client.dispatch("tcp://localhost:60196", TEST_MESSAGE, new HashMap());
               
        // poll and pull back through test service
        latch.await(getTimeout(), TimeUnit.MILLISECONDS);
        assertEquals("Received stream; length: 16; 'Test...sage'", message.get());
    }
}
