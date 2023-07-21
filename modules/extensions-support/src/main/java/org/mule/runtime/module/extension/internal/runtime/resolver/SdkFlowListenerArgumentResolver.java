/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
