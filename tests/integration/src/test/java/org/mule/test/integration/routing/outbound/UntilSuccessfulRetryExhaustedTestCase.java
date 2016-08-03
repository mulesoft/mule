/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.junit.Assert.fail;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.context.notification.ExceptionNotificationListener;
import org.mule.runtime.core.context.notification.ExceptionNotification;
import org.mule.runtime.core.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class UntilSuccessfulRetryExhaustedTestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/outbound/until-successful-retry-exhausted.xml";
    }
    
    @Test
    public void onRetryExhaustedCallExceptionStrategy() throws Exception
    {
        final Latch exceptionStrategyCalledLatch = new Latch();
        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>() {
            @Override
            public void onNotification(ExceptionNotification notification)
            {
                exceptionStrategyCalledLatch.release();
            }
        });
        flowRunner("retryExhausted").withPayload("message").run();
        if (!exceptionStrategyCalledLatch.await(10000, TimeUnit.MILLISECONDS))
        {
            fail("exception strategy was not executed");
        }
    }
}
