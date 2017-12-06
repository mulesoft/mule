/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.extension.api.runtime.process.FlowExecutorFactory;
import org.mule.runtime.module.extension.internal.runtime.operation.ImmutableFlowExecutorFactory;

/**
 * An implementation of {@link ArgumentResolver} which returns the value obtained through
 * {@link ExecutionContext#getConfiguration()}
 *
 * @since 4.1
 */
public final class FlowExecutorFactoryArgumentResolver implements ArgumentResolver<FlowExecutorFactory> {

  @Override
  public FlowExecutorFactory resolve(ExecutionContext executionContext) {
    ExecutionContextAdapter contextAdapter = (ExecutionContextAdapter) executionContext;
    return new ImmutableFlowExecutorFactory(contextAdapter.getEvent(),
                                            contextAdapter.getMuleContext().getConfigurationComponentLocator());
  }
}
