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
import org.mule.api.client.MuleClient;
import org.mule.api.context.notification.RoutingNotificationListener;
import org.mule.context.notification.RoutingNotification;
import org.mule.functional.junit4.FunctionalTestCase;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("See MULE-8830")
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
        assertTrue(result.getPayload() instanceof List);

        List<MuleMessage> results = (List<MuleMessage>) result.getPayload();
        assertEquals(2, results.size());
        for (int i = 0; i < results.size(); i++)
        {
            MuleMessage msg = results.get(i);
            assertEquals("test Received", msg.getPayload());
        }

        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
    }
}
