/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.context.notification;

import org.mule.api.client.LocalMuleClient;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import org.junit.Test;

public class FailingNotificationListenerTestCase extends FunctionalTestCase
{

    private static int count = 0;
    private static final Object lock = new Object();

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/notifications/failing-notification-listener-config.xml";
    }

    @Test
    public void testName() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        client.dispatch("vm://testInput", TEST_MESSAGE, null);
        client.dispatch("vm://testInput", TEST_MESSAGE, null);

        Prober prober = new PollingProber(1000, 10);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                return count == 2;
            }

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
