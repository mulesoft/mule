/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.execution.adapter.SdkVoidCompletionCallbackAdapter;
import org.mule.sdk.api.runtime.process.VoidCompletionCallback;;

/**
 * {@link ArgumentResolver} which resolves to a {@link VoidCompletionCallback} by delegating into a
 * {@link VoidCallbackArgumentResolver} and adapting the result.
 *
 * @since 4.5.0
 */
public final class SdkVoidCallbackArgumentResolver implements ArgumentResolver<VoidCompletionCallback> {

  private final ArgumentResolver<org.mule.runtime.extension.api.runtime.process.VoidCompletionCallback> voidCompletionCallbackArgumentResolver =
      new VoidCallbackArgumentResolver();

  @Override
  public VoidCompletionCallback resolve(ExecutionContext executionContext) {
    org.mule.runtime.extension.api.runtime.process.VoidCompletionCallback voidCompletionCallback =
        voidCompletionCallbackArgumentResolver.resolve(executionContext);
    return voidCompletionCallback == null ? null : new SdkVoidCompletionCallbackAdapter(voidCompletionCallback);
  }
}
