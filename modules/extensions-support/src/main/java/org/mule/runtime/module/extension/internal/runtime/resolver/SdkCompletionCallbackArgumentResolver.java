/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.execution.adapter.SdkCompletionCallbackAdapter;
import org.mule.sdk.api.runtime.process.CompletionCallback;

/**
 * {@link ArgumentResolver} which resolves a {@link CompletionCallback} by adapting the content of the {@link ExecutionContext}
 * variable
 * {@link org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties#COMPLETION_CALLBACK_CONTEXT_PARAM}
 *
 * @since 4.5.0
 */
public final class SdkCompletionCallbackArgumentResolver implements ArgumentResolver<CompletionCallback> {

  @Override
  public CompletionCallback resolve(ExecutionContext executionContext) {
    org.mule.runtime.extension.api.runtime.process.CompletionCallback completionCallback =
        (org.mule.runtime.extension.api.runtime.process.CompletionCallback) ((ExecutionContextAdapter) executionContext)
            .getVariable(COMPLETION_CALLBACK_CONTEXT_PARAM);
    return completionCallback == null ? null : new SdkCompletionCallbackAdapter(completionCallback);
  }
}
