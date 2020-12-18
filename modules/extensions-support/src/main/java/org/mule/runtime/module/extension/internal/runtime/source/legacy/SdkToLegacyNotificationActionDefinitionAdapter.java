/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.extension.api.notification.NotificationActionDefinition;

public class SdkToLegacyNotificationActionDefinitionAdapter implements NotificationActionDefinition {

  private final org.mule.sdk.api.notification.NotificationActionDefinition delegate;

  public SdkToLegacyNotificationActionDefinitionAdapter(org.mule.sdk.api.notification.NotificationActionDefinition<?> delegate) {
    this.delegate = delegate;
  }

  @Override
  public DataType getDataType() {
    return delegate.getDataType();
  }
}
