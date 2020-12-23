/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.notification;

import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.ExtensionNotification;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.internal.notification.ExtensionAction;
import org.mule.sdk.api.notification.NotificationActionDefinition;

import java.util.function.Supplier;

/**
 * Represents notifications fired by an extension.
 *
 * @since 4.1
 */
public class DefaultExtensionNotification implements ExtensionNotification {

  private static final long serialVersionUID = 6641419368045215517L;

  private final Event event;
  private final Component component;
  private final NotificationActionDefinition actionDefinition;
  private final Supplier<TypedValue<?>> data;

  private ExtensionAction action;

  /**
   * Creates a new {@link DefaultExtensionNotification} validating that the {@code actionDefinition} type information matches the
   * actual data to use.
   *
   * @param event the {@link Event} associated to the notification
   * @param component the {@link Component} associated to the notification
   * @param actionDefinition the {@link NotificationActionDefinition} to use
   * @param data the information to expose
   * @throws IllegalArgumentException when the {@code actionDefinition} type doesn't match {@code data}
   */
  public DefaultExtensionNotification(Event event, Component component,
                                      NotificationActionDefinition actionDefinition,
                                      TypedValue<?> data) {
    DataType actualDataType = data.getDataType();
    DataType expectedDataType = actionDefinition.getDataType();
    checkArgument(expectedDataType.isCompatibleWith(actualDataType),
                  () -> format("The action data type (%s) does not match the actual data type received (%s)",
                               expectedDataType,
                               actualDataType));
    this.event = event;
    this.component = component;
    this.actionDefinition = actionDefinition;
    this.data = () -> data;
  }

  /**
   * Creates a new {@link DefaultExtensionNotification} validating that the {@code actionDefinition} type information matches the
   * actual data to use.
   *
   * @param event the {@link Event} associated to the notification
   * @param component the {@link Component} associated to the notification
   * @param actionDefinition the {@link NotificationActionDefinition} to use
   * @param dataValue a supplier for the information to expose
   * @param actualDataType the data type of the information to expose
   * @throws IllegalArgumentException when the {@code actionDefinition} type doesn't match {@code data}
   */
  public DefaultExtensionNotification(Event event, Component component,
                                      NotificationActionDefinition actionDefinition,
                                      Supplier<?> dataValue, DataType actualDataType) {
    DataType expectedDataType = actionDefinition.getDataType();
    checkArgument(expectedDataType.isCompatibleWith(actualDataType),
                  () -> format("The action data type (%s) does not match the actual data type received (%s)",
                               expectedDataType,
                               actualDataType));
    this.event = event;
    this.component = component;
    this.actionDefinition = actionDefinition;
    this.data = new LazyValue<>(() -> new TypedValue<>(dataValue.get(), actualDataType));
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
    return data.get();
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
    return "{action={" + getAction().getNamespace() + ":" + getAction().getIdentifier() + "}, location: "
        + component.getLocation() + "}";
  }

}
