/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.impl.internal.notifications.ServerNotificationManager;
import org.mule.impl.internal.notifications.AdminNotification;
import org.mule.impl.internal.notifications.AdminNotificationListener;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOServerNotificationListener;

import java.util.Collection;

public class ServerNotificationManagerTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/specific/server-notification-manager-test.xml";
    }

    public void testRoutingConfiguration()
    {
        ServerNotificationManager manager = managementContext.getNotificationManager();
        assertTrue(manager.getEventTypes().size() > 2);
        Object iface = manager.getEventTypes().get(TestInterface.class);
        assertNotNull(iface);
        assertEquals(iface, TestEvent.class);
        iface = manager.getEventTypes().get(TestInterface2.class);
        assertNotNull(iface);
        assertEquals(iface, AdminNotification.class);        
    }

    public void testSimpleNotification() throws InterruptedException
    {
        ServerNotificationManager manager = managementContext.getNotificationManager();
        Collection listeners = manager.getListeners();
        assertEquals(3, listeners.size());
        TestListener listener = (TestListener) managementContext.getRegistry().lookupObject("listener");
        assertNotNull(listener);
        assertFalse(listener.isCalled());
        manager.fireEvent(new TestEvent());
        Thread.sleep(1000); // asynch events
        assertTrue(listener.isCalled());
    }

    public void testDisabledNotification() throws InterruptedException
    {
        ServerNotificationManager manager = managementContext.getNotificationManager();
        Collection listeners = manager.getListeners();
        assertEquals(3, listeners.size());
        TestListener2 listener2 =
                (TestListener2) managementContext.getRegistry().lookupObject("listener2");
        assertNotNull(listener2);
        assertFalse(listener2.isCalled());
        TestAdminListener adminListener =
                (TestAdminListener) managementContext.getRegistry().lookupObject("adminListener");
        assertNotNull(adminListener);
        assertFalse(adminListener.isCalled());
        manager.fireEvent(new TestAdminEvent());
        Thread.sleep(1000); // asynch events
        assertTrue(listener2.isCalled());
        assertFalse(adminListener.isCalled());
    }

    protected static interface TestInterface extends UMOServerNotificationListener
    {
        // empty
    }

    protected static interface TestInterface2 extends UMOServerNotificationListener
    {
        // empty
    }

    protected static class TestListener implements TestInterface
    {

        private boolean called = false;

        public boolean isCalled()
        {
            return called;
        }

        public void onNotification(UMOServerNotification notification)
        {
            called = true;
        }
        
    }

    protected static class TestListener2 implements TestInterface2
    {

        private boolean called = false;

        public boolean isCalled()
        {
            return called;
        }

        public void onNotification(UMOServerNotification notification)
        {
            called = true;
        }

    }

   protected static class TestAdminListener implements AdminNotificationListener
    {

        private boolean called = false;

        public boolean isCalled()
        {
            return called;
        }

        public void onNotification(UMOServerNotification notification)
        {
            called = true;
        }

    }

    protected static class TestEvent extends UMOServerNotification
    {

        public TestEvent()
        {
            super(new Object(), 0);
        }

    }

    protected static class TestAdminEvent extends AdminNotification
    {

        public TestAdminEvent()
        {
            super(null, 0);
        }

    }

}
