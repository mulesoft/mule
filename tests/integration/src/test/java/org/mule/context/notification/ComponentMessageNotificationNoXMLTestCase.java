/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.notification.ComponentMessageNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.service.Service;
import org.mule.component.simple.EchoComponent;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test ComponentNotifications/Listeners by sending events to a component. A pre and
 * post notification should be received by listeners.
 */
public class ComponentMessageNotificationNoXMLTestCase extends AbstractMuleContextTestCase
{

    protected Service service;
    protected ServerNotificationManager manager;
    protected ComponentListener componentListener;

    public ComponentMessageNotificationNoXMLTestCase()
    {
        setDisposeContextPerClass(true);
    }

    protected void configureMuleContext(MuleContextBuilder contextBuilder)
    {
        ServerNotificationManager notificationManager = new ServerNotificationManager();
        notificationManager.setNotificationDynamic(true);
        notificationManager.addInterfaceToType(ComponentMessageNotificationListener.class,
            ComponentMessageNotification.class);
        contextBuilder.setNotificationManager(notificationManager);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        setDisposeContextPerClass(true);
        componentListener = new ComponentListener();
        service = getTestService("seda", EchoComponent.class);
        if(!muleContext.isStarted()) muleContext.start();
    }

    @Test
    public void testComponentNotificationNotRegistered() throws Exception
    {
        assertFalse(componentListener.isNotified());

        service.sendEvent(MuleTestUtils.getTestEvent("test data", muleContext));

        assertFalse(componentListener.isNotified());
        assertFalse(componentListener.isBefore());
        assertFalse(componentListener.isAfter());
    }

    @Test
    public void testComponentNotification() throws Exception
    {
        // Need to configure NotificationManager as "dynamic" in order to do this.
        muleContext.registerListener(componentListener);

        assertFalse(componentListener.isNotified());

        service.sendEvent(MuleTestUtils.getTestEvent("test data", muleContext));

        // threaded processing, make sure the notifications have time to process
        Thread.sleep(100);

        assertTrue(componentListener.isNotified());
        assertTrue(componentListener.isBefore());
        assertTrue(componentListener.isAfter());
    }

    class ComponentListener implements ComponentMessageNotificationListener
    {

        private ServerNotification notification = null;

        private boolean before = false;

        private boolean after = false;

        public void onNotification(ServerNotification notification)
        {
            this.notification = notification;
            assertEquals(ComponentMessageNotification.class, notification.getClass());
            assertTrue(notification.getSource() instanceof MuleMessage);
            assertNotNull(((ComponentMessageNotification) notification).getServiceName());

            if (notification.getAction() == ComponentMessageNotification.COMPONENT_PRE_INVOKE)
            {
                before = true;
            }
            else if (notification.getAction() == ComponentMessageNotification.COMPONENT_POST_INVOKE)
            {
                after = true;
            }
        }

        public boolean isNotified()
        {
            return null != notification;
        }

        /**
         * @return the before
         */
        public boolean isBefore()
        {
            return before;
        }

        /**
         * @return the after
         */
        public boolean isAfter()
        {
            return after;
        }

    }

}
