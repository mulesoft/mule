/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.routing;

import org.mule.api.context.notification.RoutingNotificationListener;
import org.mule.context.notification.RoutingNotification;
import org.mule.routing.correlation.CorrelationTimeoutException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.ExceptionUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AsyncReplyTimeoutFailTestCase extends FunctionalTestCase
{

    private CountDownLatch latch;

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/multi-async-repy-timeout-fail.xml";
    }

    @Test
    public void testAggregatorTimeoutWithFailure() throws Exception
    {
        latch = new CountDownLatch(1);

        muleContext.registerListener(new RoutingNotificationListener<RoutingNotification>() {
            public void onNotification(RoutingNotification notification)
            {
                if (notification.getAction() == RoutingNotification.CORRELATION_TIMEOUT)
                {
                    latch.countDown();
                }
            }
        });

        String message = "test";
        try
        {
            muleContext.getClient().send("vm://distributor.queue", message, null);
        }
        catch (Exception e)
        {
            assertTrue(ExceptionUtils.getRootCause(e) instanceof CorrelationTimeoutException);
        }

        assertTrue(latch.await(3000, TimeUnit.MILLISECONDS));
    }
}
