/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.schedule;


import org.mule.api.AnnotatedObject;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.junit.Test;

public class PollScheduleNotificationTestCase extends FunctionalTestCase
{
    public static final QName NAME = new QName("http://www.mulesoft.org/schema/mule/documentation", "name");
    Prober prober = new PollingProber(5000, 100l);

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/schedule/poll-notifications-config.xml";
    }

    @Test
    public void validateNotificationsAreSent() throws InterruptedException
    {
        final MyListener listener = new MyListener();
        muleContext.getNotificationManager().addListener(listener);
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return listener.getNotifications().size() > 1 && "pollName".equals(listener.getNotifications().get(0));
            }

            @Override
            public String describeFailure()
            {
                return "The notification was never sent";
            }
        });

    }
    class MyListener implements EndpointMessageNotificationListener<EndpointMessageNotification>{

        List<String> notifications = new ArrayList<String>();

        @Override
        public void onNotification(EndpointMessageNotification notification)
        {
            notifications.add((String) ((AnnotatedObject)notification.getImmutableEndpoint()).getAnnotation(NAME));
        }

        public List<String> getNotifications()
        {
            return notifications;
        }
    }
}
