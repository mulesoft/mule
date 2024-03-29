/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.notification.legacy;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.extension.api.notification.NotificationActionDefinition;

/**
 * Adapts a {@link org.mule.sdk.api.notification.NotificationActionDefinition} into a legacy {@link NotificationActionDefinition}
 *
 * @since 4.4.0
 */
public class LegacyNotificationActionDefinitionAdapter<E extends Enum<E>> implements NotificationActionDefinition<E> {

  private final org.mule.sdk.api.notification.NotificationActionDefinition delegate;

  public LegacyNotificationActionDefinitionAdapter(org.mule.sdk.api.notification.NotificationActionDefinition<?> delegate) {
    this.delegate = delegate;
  }

  @Override
  public DataType getDataType() {
    return delegate.getDataType();
  }
}
