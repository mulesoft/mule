/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    return new DefaultNotificationEmitter(adaptedExecutionContext.getNotificationManager(),
                                          adaptedExecutionContext.getEvent(),
                                          adaptedExecutionContext.getComponent(),
                                          adaptedExecutionContext.getComponentModel());
  }
}
