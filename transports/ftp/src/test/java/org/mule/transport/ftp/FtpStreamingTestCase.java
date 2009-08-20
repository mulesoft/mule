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
import org.mule.api.MuleMessage;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalStreamingTestComponent;

import java.io.InputStream;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

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

    public void testRequest() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference messageHolder = new AtomicReference();

        EventCallback callback = new EventCallback()
        {
            public synchronized void eventReceived(MuleEventContext context, Object component)
            {
                try
                {
                    if (1 == latch.getCount())
                    {
                        messageHolder.set(context.getMessage());
                        latch.countDown();
                    }
                }
                catch (Exception e)
                {
                    fail();
                }
            }
        };

        Object component = getComponent("testComponent");
        assertTrue("FunctionalStreamingTestComponent expected", 
            component instanceof FunctionalStreamingTestComponent);
        FunctionalStreamingTestComponent ftc = (FunctionalStreamingTestComponent) component;
        ftc.setEventCallback(callback, TEST_MESSAGE.length());
       
        createFileOnFtpServer("input.txt");
               
        // poll and pull back through test service
        assertTrue(latch.await(getTimeout(), TimeUnit.MILLISECONDS));

        MuleMessage message = (MuleMessage) messageHolder.get();
        assertNotNull(message);
        assertTrue(message.getPayload() instanceof InputStream);
    }
}
