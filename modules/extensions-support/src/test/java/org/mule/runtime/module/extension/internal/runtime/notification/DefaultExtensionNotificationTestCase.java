/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.notification;

import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.module.extension.internal.runtime.client.NullComponent.NULL_COMPONENT;
import static org.mule.runtime.module.extension.internal.runtime.notification.DefaultExtensionNotificationTestCase.TestNotificationActionDefinition.REQUEST_START;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.Notification;
import org.mule.sdk.api.notification.NotificationActionDefinition;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.junit.Test;

public class DefaultExtensionNotificationTestCase {

  @Test
  @Issue("W-16288302")
  @Description("When DefaultExtensionNotification has a NullComponent and getAction() is called, a NullPointerException is not thrown and the action is initialised.")
  public void getActionWhenTheComponentIsANullComponent() {
    TypedValue<?> mockData = mock(TypedValue.class);
    when(mockData.getDataType()).thenReturn(REQUEST_START.getDataType());
    Notification notification = new DefaultExtensionNotification(null, NULL_COMPONENT, REQUEST_START, mockData);

    Notification.Action action = notification.getAction();

    assertEquals("NULL", action.getNamespace());
    assertEquals("REQUEST_START", action.getIdentifier());
  }

  public enum TestNotificationActionDefinition implements NotificationActionDefinition {

    REQUEST_START(fromType(TypedValue.class));

    private final DataType dataType;

    TestNotificationActionDefinition(DataType dataType) {
      this.dataType = dataType;
    }

    @Override
    public DataType getDataType() {
      return dataType;
    }
  }
}
