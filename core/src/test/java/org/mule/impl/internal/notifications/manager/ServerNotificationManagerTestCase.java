/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications.manager;

import org.mule.tck.AbstractMuleTestCase;

public class ServerNotificationManagerTestCase extends AbstractMuleTestCase
{

    protected Listener1 listener1;
    protected Listener2 listener2;
    protected ServiceNotificationManager manager;

    protected void doSetUp() throws Exception
    {
        listener1 = new Listener1();
        listener2 = new Listener2();
        manager = new ServiceNotificationManager();
    }

    protected void registerDefaultEvents() throws ClassNotFoundException
    {
        manager.addInterfaceToEvent(Listener1.class, SubEvent1.class);
        manager.addInterfaceToEvent(Listener2.class, Event2.class);
    }

    public void testNoListenersMeansNoEvents() throws ClassNotFoundException
    {
        registerDefaultEvents();
        assertNoEventsEnabled();
    }

    protected void assertNoEventsEnabled()
    {
        assertFalse(manager.isEventEnabled(Event1.class));
        assertFalse(manager.isEventEnabled(SubEvent1.class));
        assertFalse(manager.isEventEnabled(Event2.class));
        assertFalse(manager.isEventEnabled(SubEvent2.class));
        assertFalse(manager.isEventEnabled(Event3.class));
    }

    protected void registerDefaultListeners()
    {
        manager.addListenerSubscription(listener1, "id1");
        manager.addListener(listener2);
    }

    public void testAssociationOfInterfacesAndEvents() throws ClassNotFoundException
    {
        registerDefaultEvents();
        registerDefaultListeners();
        assertStandardEventsEnabled();
    }

    protected void assertStandardEventsEnabled()
    {
        assertFalse("only subclass accepted", manager.isEventEnabled(Event1.class));
        assertTrue("direct", manager.isEventEnabled(SubEvent1.class));
        assertTrue("direct", manager.isEventEnabled(Event2.class));
        assertTrue("via subclass", manager.isEventEnabled(SubEvent2.class));
        assertFalse("not specified at all", manager.isEventEnabled(Event3.class));
    }

    public void testDynamicResponseToDisablingEvents() throws ClassNotFoundException
    {
        registerDefaultEvents();
        registerDefaultListeners();
        assertStandardEventsEnabled();
        // now disable event 2
        manager.disableEvent(Event2.class);
        assertFalse("only subclass accepted", manager.isEventEnabled(Event1.class));
        assertTrue("direct", manager.isEventEnabled(SubEvent1.class));
        assertFalse("disabled", manager.isEventEnabled(Event2.class));
        assertFalse("no listener", manager.isEventEnabled(SubEvent2.class));
        assertFalse("not specified at all", manager.isEventEnabled(Event3.class));
        // the subclass should be blocked too
        manager.addInterfaceToEvent(Listener2.class, SubEvent2.class);
        assertFalse("only subclass accepted", manager.isEventEnabled(Event1.class));
        assertTrue("direct", manager.isEventEnabled(SubEvent1.class));
        assertFalse("disabled", manager.isEventEnabled(Event2.class));
        assertFalse("disabled", manager.isEventEnabled(SubEvent2.class));
        assertFalse("not specified at all", manager.isEventEnabled(Event3.class));
    }

    public void testDynamicResponseToDisablingInterfaces() throws ClassNotFoundException
    {
        registerDefaultEvents();
        registerDefaultListeners();
        assertStandardEventsEnabled();
        // now disable listener 1
        manager.disableInterface(Listener1.class);
        assertFalse("only subclass accepted", manager.isEventEnabled(Event1.class));
        assertFalse("disabled", manager.isEventEnabled(SubEvent1.class));
        assertTrue("direct", manager.isEventEnabled(Event2.class));
        assertTrue("via subclass", manager.isEventEnabled(SubEvent2.class));
        assertFalse("not specified at all", manager.isEventEnabled(Event3.class));
    }

    /**
     * A new policy should only be generated when the configuration changes
     */
    public void testPolicyCaching() throws ClassNotFoundException
    {
        registerDefaultEvents();
        registerDefaultListeners();
        Policy policy = manager.getPolicy();
        assertStandardEventsEnabled();
        assertSame(policy, manager.getPolicy());
        manager.disableEvent(Event2.class);
        assertNotSame(policy, manager.getPolicy());
    }

    public void testDynamicManagerDecisions() throws ClassNotFoundException
    {
        registerDefaultEvents();
        registerDefaultListeners();
        manager.setDynamic(true);
        EventDecision decision = manager.getEventDecision(Event2.class);
        assertTrue(decision.isEnabled());
        manager.disableEvent(Event2.class);
        assertFalse(decision.isEnabled());
    }

    /**
     * When the manager is not dynamic (the default), decisions should not change
     */
    public void testNonDynamicManagerDecisions() throws ClassNotFoundException
    {
        registerDefaultEvents();
        registerDefaultListeners();
        EventDecision decision = manager.getEventDecision(Event2.class);
        assertTrue(decision.isEnabled());
        manager.disableEvent(Event2.class);
        assertTrue(decision.isEnabled());
    }

    public void testNotification() throws ClassNotFoundException
    {
        registerDefaultEvents();
        registerDefaultListeners();
        assertNoListenersNotified();
        manager.notifyListeners(new Event1());
        assertNoListenersNotified();
        manager.notifyListeners(new SubEvent1());
        assertNoListenersNotified();
        manager.notifyListeners(new Event1("id1"));
        assertNoListenersNotified();
        manager.notifyListeners(new SubEvent1("id1"));
        assertTrue(listener1.isNotified());
        assertFalse(listener2.isNotified());
        manager.notifyListeners(new SubEvent2());
        assertTrue(listener1.isNotified());
        assertTrue(listener2.isNotified());
    }

    protected void assertNoListenersNotified()
    {
        assertFalse(listener1.isNotified());
        assertFalse(listener2.isNotified());
    }

}
