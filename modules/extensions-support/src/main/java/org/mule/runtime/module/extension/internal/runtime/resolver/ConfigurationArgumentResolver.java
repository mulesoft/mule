/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

/**
 * An implementation of {@link ArgumentResolver} which returns the value obtained through
 * {@link ExecutionContext#getConfiguration()}
 *
 * @since 3.7.1
 */
public final class ConfigurationArgumentResolver implements ArgumentResolver<Object> {

  @Override
  public Object resolve(ExecutionContext executionContext) {
    return executionContext.getConfiguration().map(cfg -> ((ConfigurationInstance) cfg).getValue()).orElse(null);
  }
}
