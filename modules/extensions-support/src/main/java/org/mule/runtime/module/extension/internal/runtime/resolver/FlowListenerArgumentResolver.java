/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
