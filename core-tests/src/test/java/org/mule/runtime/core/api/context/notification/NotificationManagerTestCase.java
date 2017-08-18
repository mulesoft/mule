/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.internal.context.notification.Policy;
import org.mule.runtime.core.privileged.context.notification.OptimisedNotificationHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class NotificationManagerTestCase extends AbstractMuleTestCase {

  protected Listener1 listener1;
  protected Listener2 listener2;
  protected ServerNotificationManager manager;

  @Before
  public void setUpListeners() {
    listener1 = new Listener1();
    listener2 = new Listener2();
    manager = new ServerNotificationManager();
  }

  protected void registerDefaultEvents() throws ClassNotFoundException {
    manager.addInterfaceToType(Listener1.class, SubEvent1.class);
    manager.addInterfaceToType(Listener2.class, Event2.class);
  }

  @Test
  public void testNoListenersMeansNoEvents() throws ClassNotFoundException {
    registerDefaultEvents();
    assertNoEventsEnabled();
  }

  protected void assertNoEventsEnabled() {
    assertFalse(manager.isNotificationEnabled(Event1.class));
    assertFalse(manager.isNotificationEnabled(SubEvent1.class));
    assertFalse(manager.isNotificationEnabled(SubSubEvent1.class));
    assertFalse(manager.isNotificationEnabled(Event2.class));
    assertFalse(manager.isNotificationEnabled(SubEvent2.class));
    assertFalse(manager.isNotificationEnabled(Event3.class));
  }

  protected void registerDefaultListeners() {
    manager.addListenerSubscription(listener1, notification -> "id1".equals(notification.getResourceIdentifier()));
    manager.addListener(listener2);
  }

  @Test
  public void testAssociationOfInterfacesAndEvents() throws ClassNotFoundException {
    registerDefaultEvents();
    registerDefaultListeners();
    assertStandardEventsEnabled();
  }

  protected void assertStandardEventsEnabled() {
    assertTrue("via subclass", manager.isNotificationEnabled(Event1.class));
    assertTrue("direct", manager.isNotificationEnabled(SubEvent1.class));
    assertTrue("via superclass", manager.isNotificationEnabled(SubSubEvent1.class));
    assertTrue("direct", manager.isNotificationEnabled(Event2.class));
    assertTrue("via superclass", manager.isNotificationEnabled(SubEvent2.class));
    assertFalse("not specified at all", manager.isNotificationEnabled(Event3.class));
  }

  @Test
  public void testDynamicResponseToDisablingEvents() throws ClassNotFoundException {
    registerDefaultEvents();
    registerDefaultListeners();
    assertStandardEventsEnabled();
    // now disable event 2
    manager.disableType(Event2.class);
    assertTrue("via subclass", manager.isNotificationEnabled(Event1.class));
    assertTrue("direct", manager.isNotificationEnabled(SubEvent1.class));
    assertFalse("disabled", manager.isNotificationEnabled(Event2.class));
    assertFalse("no listener", manager.isNotificationEnabled(SubEvent2.class));
    assertFalse("not specified at all", manager.isNotificationEnabled(Event3.class));
    // the subclass should be blocked too
    manager.addInterfaceToType(Listener2.class, SubEvent2.class);
    assertTrue("via subclass", manager.isNotificationEnabled(Event1.class));
    assertTrue("direct", manager.isNotificationEnabled(SubEvent1.class));
    assertFalse("disabled", manager.isNotificationEnabled(Event2.class));
    assertFalse("disabled", manager.isNotificationEnabled(SubEvent2.class));
    assertFalse("not specified at all", manager.isNotificationEnabled(Event3.class));
  }

  @Test
  public void testDynamicResponseToDisablingInterfaces() throws ClassNotFoundException {
    registerDefaultEvents();
    registerDefaultListeners();
    assertStandardEventsEnabled();
    // now disable listener 1
    manager.disableInterface(Listener1.class);
    assertFalse("via subclass, but no listener", manager.isNotificationEnabled(Event1.class));
    assertFalse("disabled", manager.isNotificationEnabled(SubEvent1.class));
    assertFalse("via superclass, but no listener", manager.isNotificationEnabled(SubSubEvent1.class));
    assertTrue("direct", manager.isNotificationEnabled(Event2.class));
    assertTrue("via superclass", manager.isNotificationEnabled(SubEvent2.class));
    assertFalse("not specified at all", manager.isNotificationEnabled(Event3.class));
  }

  /**
   * A new policy should only be generated when the configuration changes
   */
  @Test
  public void testPolicyCaching() throws ClassNotFoundException {
    registerDefaultEvents();
    registerDefaultListeners();
    Policy policy = manager.getPolicy();
    assertStandardEventsEnabled();
    assertSame(policy, manager.getPolicy());
    manager.disableType(Event2.class);
    assertNotSame(policy, manager.getPolicy());
  }

  @Test
  public void testDynamicManagerDecisions() throws ClassNotFoundException {
    registerDefaultEvents();
    registerDefaultListeners();
    manager.setNotificationDynamic(true);
    OptimisedNotificationHandler decision = new OptimisedNotificationHandler(manager, Event2.class);
    assertTrue(decision.isNotificationEnabled(Event2.class));
    manager.disableType(Event2.class);
    assertFalse(decision.isNotificationEnabled(Event2.class));
  }

  /**
   * When the manager is not dynamic (the default), decisions should not change
   */
  @Test
  public void testNonDynamicManagerDecisions() throws ClassNotFoundException {
    registerDefaultEvents();
    registerDefaultListeners();
    OptimisedNotificationHandler decision = new OptimisedNotificationHandler(manager, Event2.class);
    assertTrue(decision.isNotificationEnabled(Event2.class));
    manager.disableType(Event2.class);
    assertTrue(decision.isNotificationEnabled(Event2.class));
  }

  @Test
  public void testNotification() throws ClassNotFoundException {
    registerDefaultEvents();
    registerDefaultListeners();
    assertNoListenersNotified();
    manager.notifyListeners(new Event1(), (listener, nfn) -> listener.onNotification(nfn));
    assertNoListenersNotified();
    manager.notifyListeners(new SubEvent1(), (listener, nfn) -> listener.onNotification(nfn));
    assertNoListenersNotified();
    manager.notifyListeners(new Event1("id1"), (listener, nfn) -> listener.onNotification(nfn));
    assertNoListenersNotified();
    manager.notifyListeners(new SubSubEvent1("id1"), (listener, nfn) -> listener.onNotification(nfn));
    assertTrue(listener1.isNotified());
    assertFalse(listener2.isNotified());
    manager.notifyListeners(new Event2(), (listener, nfn) -> listener.onNotification(nfn));
    assertTrue(listener1.isNotified());
    assertTrue(listener2.isNotified());
  }

  protected void assertNoListenersNotified() {
    assertFalse(listener1.isNotified());
    assertFalse(listener2.isNotified());
  }

}
