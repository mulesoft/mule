/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.SOURCE_CALLBACK_CONTEXT_PARAM;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.ExtensionProperties;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;

/**
 * An {@link ArgumentResolver} which returns a {@link org.mule.sdk.api.runtime.source.SourceCallbackContext} registered as an
 * {@link ExecutionContext} variable under the key {@link ExtensionProperties#SOURCE_CALLBACK_CONTEXT_PARAM}.
 * <p>
 * If no such variable exists, then it returns {@code null}
 *
 * @since 4.0
 */
public class SourceCallbackContextArgumentResolver implements ArgumentResolver<SourceCallbackContext> {

  @Override
  public SourceCallbackContext resolve(ExecutionContext executionContext) {
    return ((ExecutionContextAdapter<ComponentModel>) executionContext).getVariable(SOURCE_CALLBACK_CONTEXT_PARAM);
  }
}
