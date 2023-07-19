/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.notification.NotificationEmitter;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.notification.DefaultNotificationEmitter;
import org.mule.runtime.module.extension.internal.runtime.notification.legacy.LegacyNotificationEmitterAdapter;

/**
 * {@link ArgumentResolver} implementation for {@link NotificationEmitter} parameters.
 *
 * @since 4.1
 */
public class NotificationHandlerArgumentResolver implements ArgumentResolver<NotificationEmitter> {

  private final ArgumentResolver<org.mule.sdk.api.notification.NotificationEmitter> notificationEmitterArgumentResolver =
      new SdkNotificationHandlerArgumentResolver();

  @Override
  public NotificationEmitter resolve(ExecutionContext executionContext) {
    org.mule.sdk.api.notification.NotificationEmitter notificationEmitter =
        notificationEmitterArgumentResolver.resolve(executionContext);
    return notificationEmitter == null ? null : new LegacyNotificationEmitterAdapter(notificationEmitter);
  }
}
