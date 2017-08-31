/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.FlowListener;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.operation.DefaultFlowListener;

/**
 * An {@link ArgumentResolver} which produces instances of {@link FlowListener}
 *
 * @since 4.0
 */
public class FlowListenerArgumentResolver implements ArgumentResolver<FlowListener> {

  @Override
  public FlowListener resolve(ExecutionContext executionContext) {
    return new DefaultFlowListener(executionContext.getExtensionModel(),
                                   (OperationModel) executionContext.getComponentModel(),
                                   ((EventedExecutionContext) executionContext).getEvent());
  }
}
