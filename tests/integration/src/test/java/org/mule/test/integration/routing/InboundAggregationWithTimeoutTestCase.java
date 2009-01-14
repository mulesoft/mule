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

import org.mule.api.context.notification.RoutingNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.context.notification.RoutingNotification;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class InboundAggregationWithTimeoutTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/multi-inbound-aggregator-with-timeout.xml";
    }

    public void testAggregatorTimeout() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);

        muleContext.registerListener(new RoutingNotificationListener()
        {
            public void onNotification(ServerNotification notification)
            {
                if (notification.getAction() == RoutingNotification.CORRELATION_TIMEOUT)
                {
                    latch.countDown();
                }
            }
        });

        String message = "test";
        MuleClient client = new MuleClient();
        client.dispatch("vm://distributor.queue", message, null);

        assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
    }
}