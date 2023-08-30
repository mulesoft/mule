/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.notification;

import org.mule.runtime.api.metadata.DataType;
import org.mule.sdk.api.notification.NotificationActionDefinition;

/**
 * Adapts a {@link org.mule.runtime.extension.api.notification.NotificationActionDefinition} into a
 * {@link NotificationActionDefinition}
 *
 * @since 4.5.0
 */
public class SdkNotificationActionDefinitionAdapter implements NotificationActionDefinition {

  /**
   * Returns a new instance from the given {@code value}.
   *
   * @param value either a {@link NotificationActionDefinition} or a
   *              {@link org.mule.runtime.extension.api.notification.NotificationActionDefinition}
   * @return a {@link NotificationActionDefinition}
   * @throws IllegalArgumentException if the value is not an instance of the expected types.
   */
  public static NotificationActionDefinition from(Object value) {
    if (value instanceof NotificationActionDefinition) {
      return (NotificationActionDefinition) value;
    } else if (value instanceof org.mule.runtime.extension.api.notification.NotificationActionDefinition) {
      return new SdkNotificationActionDefinitionAdapter((org.mule.runtime.extension.api.notification.NotificationActionDefinition) value);
    } else {
      throw new IllegalArgumentException("Unsupported type " + value.getClass());
    }
  }

  private final org.mule.runtime.extension.api.notification.NotificationActionDefinition delegate;

  public SdkNotificationActionDefinitionAdapter(org.mule.runtime.extension.api.notification.NotificationActionDefinition delegate) {
    this.delegate = delegate;
  }

  @Override
  public DataType getDataType() {
    return delegate.getDataType();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof SdkNotificationActionDefinitionAdapter) {
      return delegate.equals(((SdkNotificationActionDefinitionAdapter) o).delegate);
    }

    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
