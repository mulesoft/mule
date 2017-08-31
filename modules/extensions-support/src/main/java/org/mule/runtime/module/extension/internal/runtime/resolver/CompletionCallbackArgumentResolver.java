/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.module.extension.internal.ExtensionProperties;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * {@link ArgumentResolver} which returns the {@link ExtensionProperties#COMPLETION_CALLBACK_CONTEXT_PARAM}
 * context variable.
 * <p>
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
