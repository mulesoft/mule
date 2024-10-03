/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.notification;

import static org.mule.test.heisenberg.extension.HeisenbergNotificationAction.NEW_BATCH;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.Notification;
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
    AbstractComponent nullComponent = mock(AbstractComponent.class);
    when(nullComponent.getLocation()).thenReturn(null);
    TypedValue<?> typedValue = new TypedValue<>("", NEW_BATCH.getDataType());
    Notification notification = new DefaultExtensionNotification(null, nullComponent, NEW_BATCH, typedValue);

    Notification.Action action = notification.getAction();

    assertThat(action.getNamespace(), is("NULL"));
    assertThat(action.getIdentifier(), is("NEW_BATCH"));
  }
}
