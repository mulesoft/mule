/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.context.notification.ConnectorMessageNotification;
import org.mule.context.notification.ServerNotificationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class TestConnectorMessageNotificationListener implements ServerNotificationListener<ConnectorMessageNotification>
{

    private final CountDownLatch latch;
    private final String expectedExchangePoint;

    private List<String> notificationActionNames = new ArrayList<>();

    public TestConnectorMessageNotificationListener()
    {
        // Dummy listener for registration
        latch = null;
        expectedExchangePoint = null;
    }

    public TestConnectorMessageNotificationListener(CountDownLatch latch, String expectedExchangePoint)
    {
        this.latch = latch;
        this.expectedExchangePoint = expectedExchangePoint;
    }

    @Override
    public void onNotification(ConnectorMessageNotification notification)
    {
        notificationActionNames.add(notification.getActionName());
        if( latch!=null )
        {
            assertThat(notification.getEndpoint(), is(expectedExchangePoint));
            latch.countDown();
        }
    }

    public List<String> getNotificationActionNames()
    {
        return notificationActionNames;
    }

    public static ServerNotificationManager register(ServerNotificationManager serverNotificationManager)
    {
        final Map<Class<? extends ServerNotificationListener>, Set<Class<? extends ServerNotification>>> mapping = serverNotificationManager.getInterfaceToTypes();
        if (!mapping.containsKey(EndpointMessageNotificationListener.class))
        {
            serverNotificationManager.addInterfaceToType(TestConnectorMessageNotificationListener.class, ConnectorMessageNotification.class);
            serverNotificationManager.addListener(new TestConnectorMessageNotificationListener());
        }
        return serverNotificationManager;
    }


}