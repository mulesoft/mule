/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.core.context.notification;

import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.context.notification.ExceptionNotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import org.junit.Test;

public class FailingNotificationListenerTestCase extends AbstractIntegrationTestCase
{
    private static int count = 0;
    private static final Object lock = new Object();

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/notifications/failing-notification-listener-config.xml";
    }

    @Test
    public void testName() throws Exception
    {
        flowRunner("testFlow").withPayload(TEST_MESSAGE).asynchronously().run();
        flowRunner("testFlow").withPayload(TEST_MESSAGE).asynchronously().run();

        Prober prober = new PollingProber(1000, 10);
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return count == 2;
            }

            @Override
            public String describeFailure()
            {
                return "Expected to received 2 notifications but received " + count;
            }
        });
    }

    public static class ExceptionFailingListener implements ExceptionNotificationListener
    {
        @Override
        public void onNotification(ServerNotification notification)
        {
            synchronized (lock)
            {
                count = count + 1;
            }

            throw new IllegalStateException();
        }
    }
}
