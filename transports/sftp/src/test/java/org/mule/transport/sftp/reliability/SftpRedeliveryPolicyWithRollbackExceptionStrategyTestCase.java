/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp.reliability;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.context.notification.ExceptionNotification;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SftpRedeliveryPolicyWithRollbackExceptionStrategyTestCase extends SftpRedeliveryPolicyTestCase
{

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> parameters()
    {
        return getParameters();
    }

    public SftpRedeliveryPolicyWithRollbackExceptionStrategyTestCase(String name, boolean archive)
    {
        super(name, archive);
    }

    @Override
    protected String getConfigFile()
    {
        return "sftp-redelivery-with-rollback-exception-strategy.xml";
    }

    @Test
    public void testDeadLetterQueueDelivery() throws Exception
    {
        final AtomicInteger deliveredTimes = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(MAX_REDELIVERY_ATTEMPS);

        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>()
        {
            @Override
            public void onNotification(ExceptionNotification notification)
            {
                deliveredTimes.incrementAndGet();
                latch.countDown();
            }
        });

        super.testDeadLetterQueueDelivery();
        assertThat(latch.await(TIMEOUT, TimeUnit.MILLISECONDS), is(true));
        PollingProber pollingProber = new PollingProber(TIMEOUT, 100);
        pollingProber.check(new JUnitProbe()
        {
            @Override
            protected boolean test() throws Exception
            {
                // the flow has to be executed MAX_REDELIVERY_ATTEMPS + 1 times
                // also, the last notification is about the exception strategy
                // giving up and sending the message through the DLQ
                assertThat(deliveredTimes.get(), is(MAX_REDELIVERY_ATTEMPS + 2));
                return true;
            }

            @Override
            public String describeFailure()
            {
                return "Incorrect amount of notifications received";
            }
        });
    }

}
