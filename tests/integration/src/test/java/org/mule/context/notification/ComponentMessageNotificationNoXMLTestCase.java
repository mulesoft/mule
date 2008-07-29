/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
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
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;

/**
 * Test ComponentNotifications/Listeners by sending events to a component. A pre and
 * post notification should be received by listeners.
 */
public class ComponentMessageNotificationNoXMLTestCase extends AbstractMuleTestCase
{

    protected Service service;
    protected ServerNotificationManager manager;
    protected ComponentListener componentListener;

    protected void configureMuleContext(MuleContextBuilder contextBuilder)
    {
        ServerNotificationManager notificationManager = new ServerNotificationManager();
        notificationManager.setNotificationDynamic(true);
        notificationManager.addInterfaceToType(ComponentMessageNotificationListener.class,
            ComponentMessageNotification.class);
        contextBuilder.setNotificationManager(notificationManager);
    }

    protected void doSetUp() throws Exception
    {
        componentListener = new ComponentListener();
        service = getTestService("seda", EchoComponent.class);
        muleContext.start();
    }

    public void testComponentNotificationNotRegistered() throws Exception
    {
        assertFalse(componentListener.isNotified());

        service.sendEvent(MuleTestUtils.getTestInboundEvent("test data", muleContext));

        assertFalse(componentListener.isNotified());
        assertFalse(componentListener.isBefore());
        assertFalse(componentListener.isAfter());
    }

    public void testComponentNotification() throws Exception
    {
        // Need to configure NotificationManager as "dynamic" in order to do this.
        muleContext.registerListener(componentListener);

        assertFalse(componentListener.isNotified());

        service.sendEvent(MuleTestUtils.getTestInboundEvent("test data", muleContext));

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
            assertNotNull(((ComponentMessageNotification) notification).getComponent());

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
