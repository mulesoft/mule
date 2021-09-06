/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.operation.adapter.SdkFlowListenerAdapter;
import org.mule.sdk.api.runtime.operation.FlowListener;

/**
 * {@link ArgumentResolver} which resolves to a {@link FlowListener} by delegating into a {@link FlowListenerArgumentResolver} and
 * adapting the result.
 *
 * @since 4.5.0
 */
public class SdkFlowListenerArgumentResolver implements ArgumentResolver<FlowListener> {

  private final ArgumentResolver<org.mule.runtime.extension.api.runtime.operation.FlowListener> flowListenerArgumentResolver =
      new FlowListenerArgumentResolver();

  @Override
  public FlowListener resolve(ExecutionContext executionContext) {
    org.mule.runtime.extension.api.runtime.operation.FlowListener flowListener =
        flowListenerArgumentResolver.resolve(executionContext);
    return flowListener == null ? null : new SdkFlowListenerAdapter(flowListener);

  }
}
