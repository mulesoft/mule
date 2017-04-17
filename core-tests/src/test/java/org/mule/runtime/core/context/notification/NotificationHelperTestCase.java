/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context.notification;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationHandler;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class NotificationHelperTestCase extends AbstractMuleTestCase {

  @Mock
  private ServerNotificationHandler defaultNotificationHandler;

  @Mock
  private ServerNotificationManager eventNotificationHandler;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private Event event;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MuleContext muleContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private InternalMessage message;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MessageSource messageSource;

  private NotificationHelper helper;

  @Before
  public void before() {
    when(muleContext.getNotificationManager()).thenReturn(eventNotificationHandler);
    when(event.getMessage()).thenReturn(message);
    when(message.getPayload()).thenReturn(new TypedValue("", DataType.STRING));
    initMocks(eventNotificationHandler);
    initMocks(defaultNotificationHandler);

    helper = new NotificationHelper(defaultNotificationHandler, TestServerNotification.class, false);
  }

  private void initMocks(ServerNotificationHandler notificationHandler) {
    when(notificationHandler.isNotificationEnabled(TestServerNotification.class)).thenReturn(true);
    when(notificationHandler.isNotificationEnabled(ConnectorMessageNotification.class)).thenReturn(true);
  }

  @Test
  public void isNotificationEnabled() {
    assertThat(helper.isNotificationEnabled(), is(true));
    verify(defaultNotificationHandler).isNotificationEnabled(TestServerNotification.class);
  }

  @Test
  public void isNotificationEnabledForEvent() {
    assertThat(helper.isNotificationEnabled(muleContext), is(true));
    verify(eventNotificationHandler).isNotificationEnabled(TestServerNotification.class);
  }

  @Test
  public void fireNotificationForEvent() {
    final String uri = "uri";
    final FlowConstruct flowConstruct = mock(FlowConstruct.class);
    when(flowConstruct.getMuleContext()).thenReturn(muleContext);
    final int action = 100;

    helper.fireNotification(messageSource, event, uri, flowConstruct, action);
    assertConnectorMessageNotification(eventNotificationHandler, messageSource, uri, flowConstruct, action);
  }

  @Test
  public void fireSpecificNotificationForEvent() {
    TestServerNotification notification = new TestServerNotification();
    helper.fireNotification(notification, muleContext);
    verify(eventNotificationHandler).fireNotification(notification);
  }

  @Test
  public void fireSpecificNotificationOnDefaultHandler() {
    TestServerNotification notification = new TestServerNotification();
    helper.fireNotification(notification);
    verify(defaultNotificationHandler).fireNotification(notification);
  }

  private void assertConnectorMessageNotification(ServerNotificationHandler notificationHandler, MessageSource messageSource,
                                                  String uri, FlowConstruct flowConstruct, int action) {
    ArgumentCaptor<ConnectorMessageNotification> notificationCaptor = ArgumentCaptor.forClass(ConnectorMessageNotification.class);
    verify(notificationHandler).fireNotification(notificationCaptor.capture());

    ConnectorMessageNotification notification = notificationCaptor.getValue();
    assertThat(notification.getComponent(), is(messageSource));
    assertThat(notification.getAction(), is(action));
    assertThat(notification.getFlowConstruct(), is(flowConstruct));
    assertThat(notification.getEndpoint(), is(uri));
  }

  private class TestServerNotification extends ServerNotification {

    public TestServerNotification() {
      super("", 0);
    }
  }
}
