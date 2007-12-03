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

    public void testConfiguration() throws InterruptedException
    {
        ServerNotificationManager manager = managementContext.getNotificationManager();
        Object iface = manager.getEventTypes().get(TestInterface.class);
        assertNotNull(iface);
        assertEquals(iface, TestEvent.class);
        assertTrue(manager.getEventTypes().size() > 1);
        Collection listeners = manager.getListeners();
        assertEquals(1, listeners.size());
        TestListener listener = (TestListener) managementContext.getRegistry().lookupObject("listener");
        assertNotNull(listener);
        assertFalse(listener.isCalled());
        manager.fireEvent(new TestEvent());
        Thread.sleep(1000); // asynch events
        assertTrue(listener.isCalled());
    }

    protected static interface TestInterface extends UMOServerNotificationListener
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

    protected static class TestEvent extends UMOServerNotification
    {

        public TestEvent()
        {
            super(new Object(), 0);
        }

    }

}
