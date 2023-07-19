/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * {@link ArgumentResolver} which returns the
 * {@link org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties#COMPLETION_CALLBACK_CONTEXT_PARAM}
 * context variable.
 * <p/>
 * Notice that this resolver only works if the {@link ExecutionContext} is a {@link ExecutionContextAdapter}
 *
 * @since 4.0
 */
public final class CompletionCallbackArgumentResolver implements ArgumentResolver<CompletionCallback> {

  @Override
  public CompletionCallback resolve(ExecutionContext executionContext) {
    return (CompletionCallback) ((ExecutionContextAdapter) executionContext).getVariable(COMPLETION_CALLBACK_CONTEXT_PARAM);
  }
}
