/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.LocationPart;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.AbstractServerNotification;
import org.mule.runtime.api.notification.ConnectorMessageNotification;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class NotificationHelperTestCase extends AbstractMuleTestCase {

  @Mock
  private ServerNotificationManager eventNotificationHandler;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private CoreEvent event;

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

    helper = new NotificationHelper(eventNotificationHandler, TestServerNotification.class, false);
  }

  private void initMocks(ServerNotificationHandler notificationHandler) {
    when(notificationHandler.isNotificationEnabled(TestServerNotification.class)).thenReturn(true);
    when(notificationHandler.isNotificationEnabled(ConnectorMessageNotification.class)).thenReturn(true);
  }

  @Test
  public void isNotificationEnabled() {
    assertThat(helper.isNotificationEnabled(), is(true));
    verify(eventNotificationHandler).isNotificationEnabled(TestServerNotification.class);
  }

  @Test
  public void isNotificationEnabledForEvent() {
    assertThat(helper.isNotificationEnabled(), is(true));
    verify(eventNotificationHandler).isNotificationEnabled(TestServerNotification.class);
  }

  @Test
  public void fireNotificationForEvent() {
    when(messageSource.getLocation()).thenReturn(TEST_CONNECTOR_LOCATION);
    final FlowConstruct flowConstruct = mock(FlowConstruct.class, withSettings().extraInterfaces(Component.class));
    when(flowConstruct.getMuleContext()).thenReturn(muleContext);
    final int action = 100;

    helper.fireNotification(messageSource, event, TEST_CONNECTOR_LOCATION, action);
    assertConnectorMessageNotification(eventNotificationHandler, messageSource, TEST_CONNECTOR_LOCATION, action);
  }

  @Test
  public void fireSpecificNotificationForEvent() {
    TestServerNotification notification = new TestServerNotification();
    helper.fireNotification(notification);
    verify(eventNotificationHandler).fireNotification(notification);
  }

  @Test
  public void fireSpecificNotificationOnDefaultHandler() {
    TestServerNotification notification = new TestServerNotification();
    helper.fireNotification(notification);
    verify(eventNotificationHandler).fireNotification(notification);
  }

  @Test
  public void fireNotificationUsingLocation() {
    final LocationPart flowPart = mock(LocationPart.class);
    when(flowPart.getPartPath()).thenReturn("flowName");
    final ComponentLocation location = mock(ComponentLocation.class);
    when(location.getParts()).thenReturn(Collections.singletonList(flowPart));
    when(location.getComponentIdentifier()).thenReturn(TypedComponentIdentifier.builder()
        .type(SOURCE)
        .identifier(buildFromStringRepresentation("http:listener"))
        .build());
    when(messageSource.getLocation()).thenReturn(location);
    final FlowConstruct flowConstruct = mock(FlowConstruct.class, withSettings().extraInterfaces(Component.class));
    when(flowConstruct.getMuleContext()).thenReturn(muleContext);
    final int action = 100;

    helper.fireNotification(messageSource, event, location, action);
    assertConnectorMessageNotification(eventNotificationHandler, messageSource, location, action);
  }

  private void assertConnectorMessageNotification(ServerNotificationHandler notificationHandler, MessageSource messageSource,
                                                  ComponentLocation location, int action) {
    ArgumentCaptor<ConnectorMessageNotification> notificationCaptor = ArgumentCaptor.forClass(ConnectorMessageNotification.class);
    verify(notificationHandler).fireNotification(notificationCaptor.capture());

    ConnectorMessageNotification notification = notificationCaptor.getValue();
    assertThat(notification.getComponent(), is(messageSource));
    assertThat(notification.getAction().getActionId(), is(action));
    assertThat(notification.getComponent().getLocation(), is(location));
  }

  private class TestServerNotification extends AbstractServerNotification {

    public TestServerNotification() {
      super("", 0);
    }
  }
}
