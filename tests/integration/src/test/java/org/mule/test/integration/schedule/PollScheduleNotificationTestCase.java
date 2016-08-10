/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;


import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.context.notification.ConnectorMessageNotificationListener;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.notification.ConnectorMessageNotification;
import org.mule.runtime.core.source.polling.PollingMessageSource;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class PollScheduleNotificationTestCase extends AbstractIntegrationTestCase
{
    private Prober prober = new PollingProber(5000, 100l);

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/schedule/poll-notifications-config.xml";
    }

    @Test
    public void validateNotificationsAreSent() throws Exception
    {
        final MyListener listener = new MyListener();
        muleContext.getNotificationManager().addListener(listener);
        Flow flow = (Flow) getFlowConstruct("pollfoo");
        PollingMessageSource pollingMessageSource = (PollingMessageSource) flow.getMessageSource();
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return listener.getNotifications().size() > 1 && pollingMessageSource.getPollingUniqueName().equals(listener.getNotifications().get(0).getEndpoint());
            }

            @Override
            public String describeFailure()
            {
                return "The notification was never sent";
            }
        });

    }
    class MyListener implements ConnectorMessageNotificationListener<ConnectorMessageNotification>
    {

        List<ConnectorMessageNotification> notifications = new ArrayList<>();

        @Override
        public void onNotification(ConnectorMessageNotification notification)
        {
            notifications.add(notification);
        }

        public List<ConnectorMessageNotification> getNotifications()
        {
            return notifications;
        }
    }
}
