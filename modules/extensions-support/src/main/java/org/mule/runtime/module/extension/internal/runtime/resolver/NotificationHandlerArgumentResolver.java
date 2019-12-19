/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.notification.NotificationEmitter;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.notification.DefaultNotificationEmitter;

/**
 * {@link ArgumentResolver} implementation for {@link NotificationEmitter} parameters.
 *
 * @since 4.1
 */
public class NotificationHandlerArgumentResolver implements ArgumentResolver<NotificationEmitter> {

  @Override
  public NotificationEmitter resolve(ExecutionContext executionContext) {
    ExecutionContextAdapter adaptedExecutionContext = (ExecutionContextAdapter) executionContext;
    return new DefaultNotificationEmitter(adaptedExecutionContext.getMuleContext().getNotificationManager(),
                                          adaptedExecutionContext.getEvent(),
                                          adaptedExecutionContext.getComponent(),
                                          adaptedExecutionContext.getComponentModel());
  }
}
