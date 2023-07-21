/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.internal.runtime.source.legacy.SdkBackPressureContextAdapter;
import org.mule.sdk.api.runtime.source.BackPressureContext;

/**
 * {@link ArgumentResolver} which resolves to a {@link BackPressureContext} by delegating into a
 * {@link BackPressureContextArgumentResolver} and adapting the result.
 *
 * @since 4.5.0
 */
public class SdkBackPressureContextArgumentResolver implements ArgumentResolver<BackPressureContext> {

  private final ArgumentResolver<org.mule.runtime.extension.api.runtime.source.BackPressureContext> backPressureContextArgumentResolver =
      new BackPressureContextArgumentResolver();

  @Override
  public BackPressureContext resolve(ExecutionContext executionContext) {
    org.mule.runtime.extension.api.runtime.source.BackPressureContext backPressureContext =
        backPressureContextArgumentResolver.resolve(executionContext);
    return backPressureContext == null ? null : new SdkBackPressureContextAdapter(backPressureContext);
  }
}
