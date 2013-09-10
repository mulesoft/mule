/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleEventContext;
import org.mule.api.client.MuleClient;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalStreamingTestComponent;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class MultiStreamMule1692TestCase extends AbstractServiceAndFlowTestCase
{
    public static final int TIMEOUT = 3000;
    public static final String TEST_MESSAGE = "Test TCP Request";
    public static final String TEST_MESSAGE_2 = "Second test TCP Request";
    public static final String RESULT = "Received stream; length: 16; 'Test...uest'";
    public static final String RESULT_2 = "Received stream; length: 23; 'Seco...uest'";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    public MultiStreamMule1692TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "tcp-streaming-test-service.xml"},
            {ConfigVariant.FLOW, "tcp-streaming-test-flow.xml"}
        });
    }

    private EventCallback newCallback(final CountDownLatch latch, final AtomicReference<String> message)
    {
        return new EventCallback()
        {
            @Override
            public synchronized void eventReceived(MuleEventContext context, Object component)
            {
                try
                {
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
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Object ftc = getComponent("testComponent");
        assertTrue("FunctionalStreamingTestComponent expected", ftc instanceof FunctionalStreamingTestComponent);

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> message = new AtomicReference<String>();
        ((FunctionalStreamingTestComponent) ftc).setEventCallback(newCallback(latch, message), TEST_MESSAGE.length());
        client.dispatch(((InboundEndpoint) muleContext.getRegistry().lookupObject("testInbound")).getAddress(),
            TEST_MESSAGE, new HashMap<String, Object>());
        latch.await(10, TimeUnit.SECONDS);
        assertEquals(RESULT, message.get());

        final CountDownLatch latch2 = new CountDownLatch(1);
        final AtomicReference<String> message2 = new AtomicReference<String>();
        ((FunctionalStreamingTestComponent) ftc).setEventCallback(newCallback(latch2, message2), TEST_MESSAGE_2.length());
        client.dispatch(((InboundEndpoint) muleContext.getRegistry().lookupObject("testInbound")).getAddress(),
            TEST_MESSAGE_2, new HashMap<String, Object>());
        latch2.await(10, TimeUnit.SECONDS);
        assertEquals(RESULT_2, message2.get());
    }
}
