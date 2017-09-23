/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.lifecycle;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.notification.ClusterNodeNotification;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PrimaryNodeLifecycleNotificationListenerTestCase extends AbstractMuleTestCase {

  @Mock
  private NotificationListenerRegistry notificationRegistrer;
  @Mock
  private Startable mockStartable;
  @Mock
  private ClusterNodeNotification mockServerNotification;
  private StartableAndLifecycleState mockStartableAndLifecycleState;
  private StartableAndLifecycleStateEnabled mockStartableAndLifecycleStateEnabled;
  private PrimaryNodeLifecycleNotificationListener notificationListener;

  @Before
  public void setUpTest() {
    this.notificationListener = new PrimaryNodeLifecycleNotificationListener(mockStartable, notificationRegistrer);
  }

  @Test
  public void testRegister() {
    this.notificationListener.register();
    verify(notificationRegistrer, times(1)).registerListener(notificationListener);
  }

  @Test
  public void testUnregister() {
    this.notificationListener.unregister();
    verify(notificationRegistrer, times(1)).unregisterListener(notificationListener);
  }

  @Test
  public void testOnNotificationWithStartable() throws MuleException {
    this.notificationListener.onNotification(mockServerNotification);
    verify(mockStartable, times(1)).start();
  }

  @Test
  public void testOnNotificationWithLifecycleStateStarted() throws MuleException {
    mockStartableAndLifecycleState = mock(StartableAndLifecycleState.class);
    when(mockStartableAndLifecycleState.isStarted()).thenReturn(true);
    this.notificationListener =
        new PrimaryNodeLifecycleNotificationListener(mockStartableAndLifecycleState, notificationRegistrer);
    this.notificationListener.onNotification(mockServerNotification);
    verify(mockStartableAndLifecycleState, times(1)).start();
  }

  @Test
  public void testOnNotificationWithLifecycleStateStopped() throws MuleException {
    mockStartableAndLifecycleState = mock(StartableAndLifecycleState.class);
    when(mockStartableAndLifecycleState.isStarted()).thenReturn(false);
    this.notificationListener =
        new PrimaryNodeLifecycleNotificationListener(mockStartableAndLifecycleState, notificationRegistrer);
    this.notificationListener.onNotification(mockServerNotification);
    verify(mockStartableAndLifecycleState, times(0)).start();
  }

  @Test
  public void testOnNotificationWithLifecycleStateEnabledStarted() throws MuleException {
    mockStartableAndLifecycleStateEnabled = mock(StartableAndLifecycleStateEnabled.class, Answers.RETURNS_DEEP_STUBS.get());
    when(mockStartableAndLifecycleStateEnabled.getLifecycleState().isStarted()).thenReturn(true);
    this.notificationListener =
        new PrimaryNodeLifecycleNotificationListener(mockStartableAndLifecycleStateEnabled, notificationRegistrer);
    this.notificationListener.onNotification(mockServerNotification);
    verify(mockStartableAndLifecycleStateEnabled, times(1)).start();
  }

  @Test
  public void testOnNotificationWithLifecycleStateEnabledStopped() throws MuleException {
    mockStartableAndLifecycleStateEnabled = mock(StartableAndLifecycleStateEnabled.class, Answers.RETURNS_DEEP_STUBS.get());
    when(mockStartableAndLifecycleStateEnabled.getLifecycleState().isStarted()).thenReturn(false);
    this.notificationListener =
        new PrimaryNodeLifecycleNotificationListener(mockStartableAndLifecycleStateEnabled, notificationRegistrer);
    this.notificationListener.onNotification(mockServerNotification);
    verify(mockStartableAndLifecycleStateEnabled, times(0)).start();
  }

  private interface StartableAndLifecycleStateEnabled extends Startable, LifecycleStateEnabled {
  }
  private interface StartableAndLifecycleState extends Startable, LifecycleState {
  }
}
