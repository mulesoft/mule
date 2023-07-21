/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
