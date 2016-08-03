/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.context.notification.TransactionNotificationListener;
import org.mule.runtime.core.context.notification.TransactionNotification;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class TransactionalElementLifecycleTestCase extends AbstractIntegrationTestCase
{

    private static final int POLL_DELAY_MILLIS = 100;

    private List<TransactionNotification> notifications;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/transaction/transactional-lifecycle-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        notifications = new ArrayList<>();
    }

    @Test
    public void testInitializeIsCalledInInnerExceptionStrategy() throws Exception
    {
        muleContext.getNotificationManager().addListener(new TransactionNotificationListener<TransactionNotification>()
        {
            @Override
            public void onNotification(TransactionNotification notification)
            {
                notifications.add(notification);
            }
        });

        final Latch endDlqFlowLatch = new Latch();
        FunctionalTestComponent functionalTestComponent = getFunctionalTestComponent("dlq-out");
        functionalTestComponent.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                endDlqFlowLatch.release();
            }
        });
        flowRunner("in-flow").withPayload("message").run();
        if (!endDlqFlowLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("message wasn't received by dlq flow");
        }

        assertNotificationsArrived();
        assertApplicationName();
    }

    private void assertApplicationName()
    {
        for (TransactionNotification notification : notifications)
        {
            assertThat(notification.getApplicationName(), is(muleContext.getConfiguration().getId()));
        }
    }

    private void assertNotificationsArrived()
    {
        PollingProber pollingProber = new PollingProber(RECEIVE_TIMEOUT, POLL_DELAY_MILLIS);
        pollingProber.check(new JUnitProbe()
        {
            @Override
            protected boolean test() throws Exception
            {
                assertThat(notifications.size(), greaterThanOrEqualTo(2));
                return true;
            }

            @Override
            public String describeFailure()
            {
                return "Notifications did not arrive";
            }
        });
    }
}
