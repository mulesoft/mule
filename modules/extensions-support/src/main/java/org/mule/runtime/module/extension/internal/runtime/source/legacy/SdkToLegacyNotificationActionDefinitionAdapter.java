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
