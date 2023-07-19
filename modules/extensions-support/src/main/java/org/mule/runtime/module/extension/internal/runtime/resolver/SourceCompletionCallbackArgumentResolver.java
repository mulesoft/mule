/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.SOURCE_COMPLETION_CALLBACK_PARAM;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.source.SourceCompletionCallback;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.ExtensionProperties;

/**
 * An argument resolver that yields instances of {@Link SourceCompletionCallback}.
 * <p>
 * The returned instance is expected to be be set as an {@link ExecutionContext} variable under the key
 * {@link ExtensionProperties#SOURCE_COMPLETION_CALLBACK_PARAM}
 *
 * @since 4.0
 */
public class SourceCompletionCallbackArgumentResolver implements ArgumentResolver<SourceCompletionCallback> {

  @Override
  public SourceCompletionCallback resolve(ExecutionContext executionContext) {
    return (SourceCompletionCallback) ((ExecutionContextAdapter) executionContext).getVariable(SOURCE_COMPLETION_CALLBACK_PARAM);
  }
}
