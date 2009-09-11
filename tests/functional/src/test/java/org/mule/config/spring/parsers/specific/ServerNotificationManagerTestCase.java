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

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.context.notification.SecurityNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.api.security.UnauthorisedException;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.SecurityNotification;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.NullPayload;

import java.util.Collection;

public class ServerNotificationManagerTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/specific/server-notification-manager-test.xml";
    }

    public void testDynamicAttribute()
    {
        ServerNotificationManager manager = muleContext.getNotificationManager();
        assertTrue(manager.isNotificationDynamic());
    }

    public void testRoutingConfiguration()
    {
        ServerNotificationManager manager = muleContext.getNotificationManager();
        assertTrue(manager.getInterfaceToTypes().size() > 2);
        Object ifaces = manager.getInterfaceToTypes().get(TestInterface.class);
        assertNotNull(ifaces);
        assertTrue(ifaces instanceof Collection);
        assertTrue(((Collection) ifaces).contains(TestEvent.class));
        ifaces = manager.getInterfaceToTypes().get(TestInterface2.class);
        assertNotNull(ifaces);
        assertTrue(ifaces instanceof Collection);
        assertTrue(((Collection) ifaces).contains(SecurityNotification.class));
    }

    public void testSimpleNotification() throws InterruptedException
    {
        ServerNotificationManager manager = muleContext.getNotificationManager();
        Collection listeners = manager.getListeners();
        assertEquals(3, listeners.size());
        TestListener listener = (TestListener) muleContext.getRegistry().lookupObject("listener");
        assertNotNull(listener);
        assertFalse(listener.isCalled());
        manager.fireNotification(new TestEvent());
        Thread.sleep(1000); // asynch events
        assertTrue(listener.isCalled());
    }

    public void testDisabledNotification() throws InterruptedException
    {
        ServerNotificationManager manager = muleContext.getNotificationManager();
        Collection listeners = manager.getListeners();
        assertEquals(3, listeners.size());
        TestListener2 listener2 =
                (TestListener2) muleContext.getRegistry().lookupObject("listener2");
        assertNotNull(listener2);
        assertFalse(listener2.isCalled());
        TestSecurityListener adminListener =
                (TestSecurityListener) muleContext.getRegistry().lookupObject("securityListener");
        assertNotNull(adminListener);
        assertFalse(adminListener.isCalled());
        manager.fireNotification(new TestSecurityEvent(muleContext));
        Thread.sleep(1000); // asynch events
        assertTrue(listener2.isCalled());
        assertFalse(adminListener.isCalled());
    }

    protected static interface TestInterface extends ServerNotificationListener
    {
        // empty
    }

    protected static interface TestInterface2 extends ServerNotificationListener
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

        public void onNotification(ServerNotification notification)
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

        public void onNotification(ServerNotification notification)
        {
            called = true;
        }

    }

    protected static class TestSecurityListener implements SecurityNotificationListener<SecurityNotification>
    {

        private boolean called = false;

        public boolean isCalled()
        {
            return called;
        }

        public void onNotification(SecurityNotification notification)
        {
            called = true;
        }

    }

    protected static class TestEvent extends ServerNotification
    {

        public TestEvent()
        {
            super(new Object(), 0);
        }

    }

    protected static class TestSecurityEvent extends SecurityNotification
    {

        public TestSecurityEvent(MuleContext muleContext)
        {
            super(new UnauthorisedException(CoreMessages.createStaticMessage("dummy"),
                    new DefaultMuleMessage(NullPayload.getInstance(), muleContext)), 0);
        }

    }


}
