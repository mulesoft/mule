/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.context.notification.ExceptionNotification;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Test;

public class RollbackExceptionStrategyRedeliveryCountTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/rollback-exception-strategy-redelivery-count.xml";
    }

    @Test
    public void testRollbackExceptionStrategyNumberOfRetries() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(4);
        LocalMuleClient client = muleContext.getClient();
        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>() {
            @Override
            public void onNotification(ExceptionNotification notification)
            {
                latch.countDown();
            }
        });
        client.dispatch("vm://in8", "test", null);
        if (!latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("message should have been delivered at least 5 times");
        }
        MuleMessage response = client.request("vm://dlqCounter", 20000);
        assertThat(response, IsNull.notNullValue());
        AtomicInteger counter = muleContext.getRegistry().lookupObject("counter");
        assertThat(counter.get(), Is.is(4));
    }

}
