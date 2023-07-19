/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;

/**
 * {@link ArgumentResolver} for the parameters that are of {@link ParameterResolver} type.
 *
 * @since 4.0
 */
public final class ParameterResolverArgumentResolver<T> implements ArgumentResolver<ParameterResolver<T>> {

  private final ByParameterNameArgumentResolver argumentResolver;

  public ParameterResolverArgumentResolver(String parameterName) {
    argumentResolver = new ByParameterNameArgumentResolver<>(parameterName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ParameterResolver<T> resolve(ExecutionContext executionContext) {
    Object value = argumentResolver.resolve(executionContext);
    return ParameterResolver.class.isInstance(value)
        ? (ParameterResolver<T>) value
        : new StaticParameterResolver<>((T) value);
  }
}
