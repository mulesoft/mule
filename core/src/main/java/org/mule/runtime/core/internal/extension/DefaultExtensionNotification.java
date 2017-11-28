/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.extension;

import static java.lang.String.format;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.ExtensionNotification;
import org.mule.runtime.extension.api.notification.NotificationActionDefinition;
import org.mule.runtime.extension.internal.notification.ExtensionAction;

/**
 * Represents notifications fired by an extension.
 *
 * @since 1.1
 */
public class DefaultExtensionNotification implements ExtensionNotification {

  private static final long serialVersionUID = 6641419368045215517L;

  private final Event event;
  private final Component component;
  private final NotificationActionDefinition actionDefinition;
  private final TypedValue<?> data;

  private ExtensionAction action;

  public DefaultExtensionNotification(Event event, Component component,
                                      NotificationActionDefinition actionDefinition,
                                      TypedValue<?> data) {
    DataType actualDataType = data.getDataType();
    DataType expectedDataType = fromType(actionDefinition.getDataType());
    checkArgument(expectedDataType.isCompatibleWith(actualDataType),
                  format("The action data type (%s) does not match the actual data type received (%s)",
                         expectedDataType,
                         actualDataType));
    this.event = event;
    this.component = component;
    this.actionDefinition = actionDefinition;
    this.data = data;
  }

  @Override
  public Event getEvent() {
    return event;
  }

  @Override
  public Component getComponent() {
    return component;
  }

  @Override
  public TypedValue<?> getData() {
    return data;
  }

  @Override
  public Action getAction() {
    if (action == null) {
      action = new ExtensionAction(component.getLocation().getComponentIdentifier().getIdentifier().getNamespace().toUpperCase(),
                                   ((Enum) actionDefinition).name());
    }
    return action;
  }

  @Override
  public String toString() {
    return "{action={" + getAction().getNamespace() + ":" + getAction().getId() + "}, location: " + component.getLocation() + "}";
  }

}
