/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.execution.adapter.SdkRouterCompletionCallback;
import org.mule.sdk.api.runtime.process.RouterCompletionCallback;

/**
 * {@link ArgumentResolver} which returns a {@link RouterCompletionCallback}
 *
 * @since 4.4.0
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
