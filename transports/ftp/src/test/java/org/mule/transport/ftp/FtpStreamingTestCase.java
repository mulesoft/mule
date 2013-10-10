/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalStreamingTestComponent;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class FtpStreamingTestCase extends AbstractFtpServerTestCase
{
    public FtpStreamingTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "ftp-streaming-test-service.xml"},
            {ConfigVariant.FLOW, "ftp-streaming-test-flow.xml"}
        });
    }      

    @Test
    public void testRequest() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<MuleMessage> messageHolder = new AtomicReference<MuleMessage>();

        EventCallback callback = new EventCallback()
        {
            @Override
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

        MuleMessage message = messageHolder.get();
        assertNotNull(message);
        assertTrue(message.getPayload() instanceof InputStream);
    }
}
