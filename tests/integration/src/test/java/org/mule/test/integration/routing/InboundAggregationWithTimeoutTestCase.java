/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import org.mule.api.context.notification.RoutingNotificationListener;
import org.mule.context.notification.RoutingNotification;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class InboundAggregationWithTimeoutTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/multi-inbound-aggregator-with-timeout.xml";
    }

    @Test
    public void testAggregatorTimeout() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);

        muleContext.registerListener(new RoutingNotificationListener<RoutingNotification>()
        {
            public void onNotification(RoutingNotification notification)
            {
                if (notification.getAction() == RoutingNotification.CORRELATION_TIMEOUT)
                {
                    latch.countDown();
                }
            }
        });

        String message = "test";
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://distributor.queue", message, null);

        assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
    }
}
