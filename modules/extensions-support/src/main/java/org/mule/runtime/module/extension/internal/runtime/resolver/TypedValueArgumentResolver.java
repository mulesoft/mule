/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.metadata.DefaultTypedValue;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

/**
 * An implementation of {@link ArgumentResolver} which resolves the {@link TypedValue} of a parameter
 *
 * @param <T> the type of the argument to be resolved
 * @since 4.0
 */
public final class TypedValueArgumentResolver<T> implements ArgumentResolver<TypedValue<T>> {

  private final ByParameterNameArgumentResolver argumentResolver;

  public TypedValueArgumentResolver(String parameterName) {
    argumentResolver = new ByParameterNameArgumentResolver<>(parameterName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TypedValue<T> resolve(ExecutionContext executionContext) {
    Object value = argumentResolver.resolve(executionContext);
    if (value instanceof TypedValue) {
      return (TypedValue<T>) value;
    } else {
      return new DefaultTypedValue<>((T) value, DataType.fromObject(value));
    }
  }
}
