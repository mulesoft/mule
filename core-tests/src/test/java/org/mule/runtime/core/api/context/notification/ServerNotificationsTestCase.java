/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STOPPED;
import static org.mule.runtime.core.api.context.notification.ServerNotificationsTestCase.DummyNotification.EVENT_RECEIVED;

import org.mule.runtime.api.notification.CustomNotificationListener;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import java.util.EventObject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerNotificationsTestCase extends AbstractMuleContextTestCase implements MuleContextNotificationListener {

  private final AtomicBoolean managerStopped = new AtomicBoolean(false);
  private final AtomicInteger managerStoppedEvents = new AtomicInteger(0);
  private final AtomicInteger customNotificationCount = new AtomicInteger(0);

  public ServerNotificationsTestCase() {
    setStartContext(true);
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    managerStopped.set(true);
    managerStoppedEvents.set(0);
  }

  @Test
  public void testStandardNotifications() throws Exception {
    getNotificationListenerRegistry().registerListener(this);
    muleContext.stop();
    assertTrue(managerStopped.get());
  }

  @Test
  public void testMultipleRegistrations() throws Exception {
    getNotificationListenerRegistry().registerListener(this);
    getNotificationListenerRegistry().registerListener(this);
    muleContext.stop();
    assertTrue(managerStopped.get());
    assertEquals(1, managerStoppedEvents.get());
  }

  @Test
  public void testUnregistering() throws Exception {
    getNotificationListenerRegistry().registerListener(this);
    getNotificationListenerRegistry().unregisterListener(this);
    muleContext.stop();
    // these should still be false because we unregistered ourselves
    assertFalse(managerStopped.get());
  }

  @Test
  public void testMismatchingUnregistrations() throws Exception {
    // this has changed in 2.x. now, unregistering removes all related entries
    getNotificationListenerRegistry().registerListener(this);
    DummyListener dummy = new DummyListener();
    getNotificationListenerRegistry().registerListener(dummy);
    getNotificationListenerRegistry().registerListener(dummy);
    getNotificationListenerRegistry().unregisterListener(dummy);
    muleContext.stop();

    assertTrue(managerStopped.get());
    assertEquals(1, managerStoppedEvents.get());
  }

  @Test
  public void testCustomNotifications() throws Exception {
    final CountDownLatch latch = new CountDownLatch(2);

    getNotificationListenerRegistry().registerListener((DummyNotificationListener) notification -> {
      if (new IntegerAction(EVENT_RECEIVED).equals(notification.getAction())) {
        customNotificationCount.incrementAndGet();
        assertEquals("hello", ((EventObject) notification).getSource());
        latch.countDown();
      }
    });

    getNotificationDispatcher().dispatch(new DummyNotification("hello", EVENT_RECEIVED));
    getNotificationDispatcher().dispatch(new DummyNotification("hello", EVENT_RECEIVED));

    // Wait for the notifcation event to be fired as they are queued
    latch.await(2000, MILLISECONDS);
    assertEquals(2, customNotificationCount.get());
  }

  private NotificationListenerRegistry getNotificationListenerRegistry() throws RegistrationException {
    return getResgistry().lookupObject(NotificationListenerRegistry.class);
  }

  private NotificationDispatcher getNotificationDispatcher() throws RegistrationException {
    return ((MuleContextWithRegistries) muleContext).getRegistry().lookupObject(NotificationDispatcher.class);
  }

  private MuleRegistry getResgistry() {
    return ((MuleContextWithRegistries) muleContext).getRegistry();
  }

  @Override
  public boolean isBlocking() {
    return false;
  }

  @Override
  public void onNotification(Notification notification) {
    if (new IntegerAction(CONTEXT_STOPPED).equals(notification.getAction())) {
      managerStopped.set(true);
      managerStoppedEvents.incrementAndGet();
    }
  }

  public interface DummyNotificationListener extends CustomNotificationListener {
    // no methods
  }

  public class DummyNotification extends org.mule.runtime.api.notification.CustomNotification {

    /**
     * Serial version
     */
    private static final long serialVersionUID = -1117307108932589331L;

    public static final int EVENT_RECEIVED = -999999;

    public DummyNotification(String message, int action) {
      super(message, action);
      resourceIdentifier = message;
    }
  }
}
