/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.notification.DefaultNotificationEmitter;
import org.mule.sdk.api.notification.NotificationEmitter;

/**
 * {@link ArgumentResolver} implementation for {@link NotificationEmitter} parameters.
 *
 * @since 4.5.0
 */
public class SdkNotificationHandlerArgumentResolver implements ArgumentResolver<NotificationEmitter> {

  @Override
  public NotificationEmitter resolve(ExecutionContext executionContext) {
    ExecutionContextAdapter adaptedExecutionContext = (ExecutionContextAdapter) executionContext;
    return new DefaultNotificationEmitter(adaptedExecutionContext.getMuleContext().getNotificationManager(),
                                          adaptedExecutionContext.getEvent(),
                                          adaptedExecutionContext.getComponent(),
                                          adaptedExecutionContext.getComponentModel());
  }
}
