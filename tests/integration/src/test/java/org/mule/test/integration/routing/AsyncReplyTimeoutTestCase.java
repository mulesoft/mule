/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing;

import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.context.notification.RoutingNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.context.notification.RoutingNotification;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class AsyncReplyTimeoutTestCase extends FunctionalTestCase
{
    private CountDownLatch latch;
    
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/multi-async-repy-timeout.xml";
    }

    public void testAggregatorTimeoutWithoutFailure() throws Exception
    {
        latch = new CountDownLatch(1);

        muleContext.registerListener(new RoutingNotificationListener() 
        {
            public void onNotification(ServerNotification notification)
            {
                if(notification.getAction() == RoutingNotification.MISSED_ASYNC_REPLY)
                {
                    latch.countDown();
                    assertEquals("test Received Late!", ((MuleMessage)((RoutingNotification)notification).getSource()).getPayload());
                }
            }
        });

        String message = "test";
        MuleClient client = new MuleClient();
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
