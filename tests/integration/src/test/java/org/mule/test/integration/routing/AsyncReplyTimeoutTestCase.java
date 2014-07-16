/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.client.MuleClient;
import org.mule.api.context.notification.RoutingNotificationListener;
import org.mule.context.notification.RoutingNotification;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class AsyncReplyTimeoutTestCase extends FunctionalTestCase
{
    private CountDownLatch latch;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/multi-async-repy-timeout.xml";
    }

    @Test
    public void testAggregatorTimeoutWithoutFailure() throws Exception
    {
        latch = new CountDownLatch(1);

        muleContext.registerListener(new RoutingNotificationListener<RoutingNotification>()
        {
            @Override
            public void onNotification(RoutingNotification notification)
            {
                if (notification.getAction() == RoutingNotification.MISSED_AGGREGATION_GROUP_EVENT)
                {
                    latch.countDown();
                    assertEquals("test Received Late!", ((MuleMessage)notification.getSource()).getPayload());
                }
            }
        });

        String message = "test";
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://distributor.queue", message, null);
        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);

        MuleMessageCollection mc = (MuleMessageCollection)result;
        assertEquals(2, mc.size());
        for (int i = 0; i < mc.getMessagesAsArray().length; i++)
        {
            MuleMessage msg = mc.getMessagesAsArray()[i];
            assertEquals("test Received", msg.getPayload());
        }

        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
    }
}
