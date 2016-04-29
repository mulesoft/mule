/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.context.notification.EndpointMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.context.notification.ConnectorMessageNotification;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;

public class TestConnectorMessageNotificationListener implements ServerNotificationListener<ConnectorMessageNotification>
{

    private final CountDownLatch latch;
    private final String expectedExchangePoint;

    private List<ConnectorMessageNotification> notifications = new ArrayList<>();

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
        notifications.add(notification);
        if (latch != null)
        {
            assertThat(notification.getEndpoint(), is(expectedExchangePoint));
            latch.countDown();
        }
    }

    public List<String> getNotificationActionNames()
    {
        return (List<String>) CollectionUtils.collect(notifications, new Transformer()
        {
            @Override
            public Object transform(Object input)
            {
                return ((ConnectorMessageNotification) input).getActionName();
            }
        });
    }

    /**
     * Gets the list of notifications for the action name.
     * @param actionName
     * @return The notifications sent for the given action.
     */
    public List<ConnectorMessageNotification> getNotifications(final String actionName)
    {
        return (List<ConnectorMessageNotification>) CollectionUtils.select(notifications, new Predicate()
        {
            @Override
            public boolean evaluate(Object object)
            {
                return ((ConnectorMessageNotification) object).getActionName().equals(actionName);
            }
        });
    }

    public static ServerNotificationManager register(ServerNotificationManager serverNotificationManager)
    {
        final Map<Class<? extends ServerNotificationListener>, Set<Class<? extends ServerNotification>>> mapping = serverNotificationManager.getInterfaceToTypes();
        if (!mapping.containsKey(EndpointMessageNotificationListener.class))
        // if (!mapping.containsKey(ConnectorMessageNotificationListener.class))
        {
            serverNotificationManager.addInterfaceToType(TestConnectorMessageNotificationListener.class, ConnectorMessageNotification.class);
            serverNotificationManager.addListener(new TestConnectorMessageNotificationListener());
        }
        return serverNotificationManager;
    }


}