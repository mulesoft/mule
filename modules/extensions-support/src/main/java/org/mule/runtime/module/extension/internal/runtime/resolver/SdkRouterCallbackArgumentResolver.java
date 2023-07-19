/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.execution.adapter.SdkRouterCompletionCallback;
import org.mule.sdk.api.runtime.process.RouterCompletionCallback;

/**
 * {@link ArgumentResolver} which returns a {@link RouterCompletionCallback}
 *
 * @since 4.5.0
 */
public final class SdkRouterCallbackArgumentResolver implements ArgumentResolver<RouterCompletionCallback> {

  private final ArgumentResolver<org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback> routerCallbackArgumentResolver =
      new RouterCallbackArgumentResolver();

  @Override
  public RouterCompletionCallback resolve(ExecutionContext executionContext) {
    org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback routerCompletionCallback =
        routerCallbackArgumentResolver.resolve(executionContext);
    return routerCompletionCallback == null ? null : new SdkRouterCompletionCallback(routerCompletionCallback);
  }
}
