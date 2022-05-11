/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.notification;

import static java.lang.String.format;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.notification.HasNotifications;
import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.sdk.api.notification.NotificationActionDefinition;
import org.mule.sdk.api.notification.NotificationEmitter;

import java.util.function.Supplier;

/**
 * Default implementation of {@link NotificationEmitter}.
 *
 * @since 4.1
 */
public class DefaultNotificationEmitter implements NotificationEmitter {

  private final ServerNotificationManager notificationManager;
  private final CoreEvent event;
  private final Component component;
  private final HasNotifications componentModel;

  public DefaultNotificationEmitter(ServerNotificationManager notificationManager, CoreEvent event,
                                    Component component, ComponentModel componentModel) {
    this.notificationManager = notificationManager;
    this.event = event;
    this.component = component;
    this.componentModel = (HasNotifications) componentModel;
  }

  @Override
  public void fire(NotificationActionDefinition action, TypedValue<?> data) {
    validateAction((Enum) action);
    notificationManager.fireNotification(new DefaultExtensionNotification(event, component, action, data));
  }

  @Override
  public void fireLazy(NotificationActionDefinition action, Supplier<?> data, DataType actualDataType) {
    validateAction((Enum) action);
    notificationManager.fireNotification(new DefaultExtensionNotification(event, component, action, data, actualDataType));
  }

  private void validateAction(Enum action) {
    String actionName = action.name();

    for (NotificationModel nm : componentModel.getNotificationModels()) {
      if (actionName.equals(nm.getIdentifier())) {
        return;
      }
    }

    throw new IllegalArgumentException(format("Cannot fire '%s' notification since it's not declared by the component.",
                                              actionName));
  }

}
