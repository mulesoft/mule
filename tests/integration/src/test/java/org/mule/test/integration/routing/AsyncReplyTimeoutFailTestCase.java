/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static org.junit.Assert.assertTrue;

import org.mule.api.context.notification.RoutingNotificationListener;
import org.mule.context.notification.RoutingNotification;
import org.mule.routing.correlation.CorrelationTimeoutException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.ExceptionUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class AsyncReplyTimeoutFailTestCase extends FunctionalTestCase
{
    private CountDownLatch latch;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/multi-async-repy-timeout-fail.xml";
    }

    @Test
    public void testAggregatorTimeoutWithFailure() throws Exception
    {
        latch = new CountDownLatch(1);

        muleContext.registerListener(new RoutingNotificationListener<RoutingNotification>()
        {
            @Override
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
