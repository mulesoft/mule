/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.routing.outbound;

import org.junit.Test;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.construct.Flow;
import org.mule.context.notification.ExceptionNotification;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

public class UntilSuccessfulRetryExhaustedTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
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
        Flow flow = (Flow) getFlowConstruct("retryExhausted");
        flow.process(getTestEvent("message"));
        if (!exceptionStrategyCalledLatch.await(10000, TimeUnit.MILLISECONDS))
        {
            fail("exception strategy was not executed");
        }
    }
}
