/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
