/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleContext;
import org.mule.api.context.notification.EndpointMessageNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.context.notification.MessageExchangeNotification;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerNotificationsTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");
    @Rule
    public SystemProperty path = new SystemProperty("path", "path");


    @Override
    protected String getConfigFile()
    {
        return "http-listener-notifications-config.xml";
    }

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        return addListenerToNotificationMappings(super.createMuleContext());
    }

    private MuleContext addListenerToNotificationMappings(MuleContext muleContext)
    {
        final Map<Class<? extends ServerNotificationListener>, Set<Class<? extends ServerNotification>>> mapping = muleContext.getNotificationManager().getInterfaceToTypes();
        if (!mapping.containsKey(EndpointMessageNotificationListener.class))
        {
            muleContext.getNotificationManager().addInterfaceToType(
                    TestMessageExchangeNotificationListener.class,
                    MessageExchangeNotification.class);
        }
        return muleContext;
    }

    @Test
    public void receiveNotification() throws Exception
    {
        CountDownLatch latch = new CountDownLatch(2);
        TestMessageExchangeNotificationListener listener = new TestMessageExchangeNotificationListener(latch);
        muleContext.getNotificationManager().addListener(listener);

        Request.Post(getListenerUrl()).execute();

        latch.await(1000, TimeUnit.MILLISECONDS);

        assertThat(listener.getNotificationActionNames(), contains("receive", "response"));
    }

    private String getListenerUrl()
    {
        return String.format("http://localhost:%s/%s", listenPort.getNumber(), path.getValue());
    }

    class TestMessageExchangeNotificationListener implements ServerNotificationListener<MessageExchangeNotification>
    {

        private final CountDownLatch latch;
        private List<String> notificationActionNames = new ArrayList<>();

        public TestMessageExchangeNotificationListener(CountDownLatch latch)
        {
            this.latch = latch;
        }

        @Override
        public void onNotification(MessageExchangeNotification notification)
        {
            if (notification.getClass().equals(MessageExchangeNotification.class))
            {
                notificationActionNames.add(notification.getActionName());
                latch.countDown();
            }
        }

        public List<String> getNotificationActionNames()
        {
            return notificationActionNames;
        }
    }

}