/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.notification;

import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.module.extension.internal.runtime.notification.DefaultExtensionNotificationTestCase.NullComponent.NULL_COMPONENT;
import static org.mule.runtime.module.extension.internal.runtime.notification.DefaultExtensionNotificationTestCase.TestNotificationActionDefinition.REQUEST_START;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.Notification;
import org.mule.sdk.api.notification.NotificationActionDefinition;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.junit.Test;

public class DefaultExtensionNotificationTestCase extends AbstractMuleTestCase {

  @Test
  @Issue("W-16288302")
  @Description("When DefaultExtensionNotification calls getAction() with a NullComponent, " +
      "component.getLocation().getComponentIdentifier() does not throw a NullPointerException and the action is created.")
  public void getActionWhenTheComponentIsANullComponent() {
    TypedValue<?> mockData = mock(TypedValue.class);
    when(mockData.getDataType()).thenReturn(REQUEST_START.getDataType());
    Notification notification = new DefaultExtensionNotification(null, NULL_COMPONENT, REQUEST_START, mockData);

    Notification.Action action = notification.getAction();

    assertThat(action.getNamespace(), is("NULL"));
    assertThat(action.getIdentifier(), is("REQUEST_START"));
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

  public static class NullComponent extends AbstractComponent implements Initialisable {

    public static final NullComponent NULL_COMPONENT = new NullComponent();

    @Override
    public void initialise() throws InitialisationException {

    }
  }
}
