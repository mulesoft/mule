/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static org.mule.runtime.api.notification.AbstractServerNotification.CUSTOM_EVENT_ACTION_START_RANGE;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STOPPED;
import static org.mule.runtime.core.api.context.notification.ServerNotificationsTestCase.DummyNotification.EVENT_RECEIVED;
import static org.mule.tck.util.MuleContextUtils.getNotificationDispatcher;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.notification.CustomNotification;
import org.mule.runtime.api.notification.CustomNotificationListener;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.EventObject;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

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
    assertThat(managerStopped.get(), is(true));
  }

  @Test
  public void testMultipleRegistrations() throws Exception {
    getNotificationListenerRegistry().registerListener(this);
    getNotificationListenerRegistry().registerListener(this);
    muleContext.stop();
    assertThat(managerStopped.get(), is(true));
    assertThat(managerStoppedEvents.get(), is(1));
  }

  @Test
  public void testUnregistering() throws Exception {
    getNotificationListenerRegistry().registerListener(this);
    getNotificationListenerRegistry().unregisterListener(this);
    muleContext.stop();
    // these should still be false because we unregistered ourselves
    assertThat(managerStopped.get(), is(false));
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

    assertThat(managerStopped.get(), is(true));
    assertThat(managerStoppedEvents.get(), is(1));
  }

  @Test
  public void testCustomNotifications() throws Exception {
    final CountDownLatch latch = new CountDownLatch(2);

    getNotificationListenerRegistry().registerListener((DummyNotificationListener) notification -> {
      if (new IntegerAction(EVENT_RECEIVED).equals(notification.getAction())) {
        customNotificationCount.incrementAndGet();
        assertThat(((EventObject) notification).getSource(), is("hello"));
        latch.countDown();
      }
    });

    getNotificationDispatcher(muleContext).dispatch(new DummyNotification("hello", EVENT_RECEIVED));
    getNotificationDispatcher(muleContext).dispatch(new DummyNotification("hello", EVENT_RECEIVED));

    // Wait for the notifcation event to be fired as they are queued
    latch.await(2000, MILLISECONDS);
    assertThat(customNotificationCount.get(), is(2));
  }

  @Test
  public void testAsyncNotificationRejectedExecution() {
    MuleContext muleContext = mock(MuleContext.class);
    SchedulerService schedulerService = mock(SchedulerService.class);
    Scheduler scheduler = mock(Scheduler.class);
    when(muleContext.getSchedulerService()).thenReturn(schedulerService);
    when(schedulerService.cpuLightScheduler()).thenReturn(scheduler);
    when(scheduler.submit(any(Callable.class))).thenThrow(new RejectedExecutionException());

    Notification notification = mock(CustomNotification.class);
    when(notification.isSynchronous()).thenReturn(false);
    NotificationListener notificationListener = mock(CustomNotificationListener.class);

    ServerNotificationManager manager = new ServerNotificationManager();
    manager.setMuleContext(muleContext);
    manager.addInterfaceToType(CustomNotificationListener.class, CustomNotification.class);
    manager.addListener(notificationListener);

    manager.fireNotification(notification);

    verify(notificationListener, never()).onNotification(notification);
  }

  @Test
  public void testSyncNotificationException() {
    ServerNotificationManager manager = new ServerNotificationManager();
    manager.setMuleContext(muleContext);
    manager.addInterfaceToType(CustomNotificationListener.class, CustomNotification.class);

    Notification notification = mock(CustomNotification.class);
    when(notification.isSynchronous()).thenReturn(true);
    NotificationListener notificationListener = mock(CustomNotificationListener.class);
    doThrow(new IllegalArgumentException()).when(notificationListener).onNotification(any(CustomNotification.class));
    manager.addListener(notificationListener);

    manager.fireNotification(notification);

    verify(notificationListener, times(1)).onNotification(notification);
  }

  @Test
  public void testSyncNotificationError() {
    ServerNotificationManager manager = new ServerNotificationManager();
    manager.setMuleContext(muleContext);
    manager.addInterfaceToType(CustomNotificationListener.class, CustomNotification.class);

    Notification notification = mock(CustomNotification.class);
    when(notification.isSynchronous()).thenReturn(true);
    NotificationListener notificationListener = mock(CustomNotificationListener.class);
    doThrow(new LinkageError()).when(notificationListener).onNotification(any(CustomNotification.class));
    manager.addListener(notificationListener);

    manager.fireNotification(notification);

    verify(notificationListener, times(1)).onNotification(notification);
  }

  @Test
  public void testCustomNotification() {
    String testMessage = "Some message";
    assertThrows(IllegalArgumentException.class, () -> new CustomNotification(testMessage, CUSTOM_EVENT_ACTION_START_RANGE - 1));
    assertThrows(IllegalArgumentException.class,
                 () -> new CustomNotification(testMessage, CUSTOM_EVENT_ACTION_START_RANGE - 1, "1"));

    CustomNotification customNotification = new CustomNotification(testMessage, CUSTOM_EVENT_ACTION_START_RANGE + 1);
    assertThat(customNotification.getEventName(), is("CustomNotification"));
    assertThat(customNotification.toString(),
               startsWith("CustomNotification{action=none, resourceId=null, serverId=null, timestamp="));
  }

  private NotificationListenerRegistry getNotificationListenerRegistry() throws RegistrationException {
    return getResgistry().lookupObject(NotificationListenerRegistry.class);
  }

  private MuleRegistry getResgistry() {
    return ((MuleContextWithRegistry) muleContext).getRegistry();
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

    public static final int EVENT_RECEIVED = NO_ACTION_ID;

    public DummyNotification(String message, int action) {
      super(message, action);
      resourceIdentifier = message;
    }
  }
}
