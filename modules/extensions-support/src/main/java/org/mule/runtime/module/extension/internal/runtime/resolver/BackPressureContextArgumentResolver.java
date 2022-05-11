/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.extension.api.runtime.source.BackPressureAction.FAIL;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.BACK_PRESSURE_ACTION_CONTEXT_PARAM;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.source.BackPressureAction;
import org.mule.runtime.extension.api.runtime.source.BackPressureContext;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.source.ImmutableBackPressureContext;

/**
 * Resolves an instance of {@link BackPressureContext}
 *
 * @since 1.1
 */
public class BackPressureContextArgumentResolver implements ArgumentResolver<BackPressureContext> {

  private final ArgumentResolver<SourceCallbackContext> callbackContextResolver =
      new LegacySourceCallbackContextArgumentResolver();

  @Override
  public BackPressureContext resolve(ExecutionContext executionContext) {
    ExecutionContextAdapter ctx = (ExecutionContextAdapter) executionContext;
    BackPressureAction action = (BackPressureAction) ctx.getVariable(BACK_PRESSURE_ACTION_CONTEXT_PARAM);
    if (action == null) {
      action = FAIL;
    }

    return new ImmutableBackPressureContext(ctx.getEvent(), action, callbackContextResolver.resolve(ctx));
  }
}
