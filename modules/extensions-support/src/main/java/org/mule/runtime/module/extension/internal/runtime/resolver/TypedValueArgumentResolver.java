/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

/**
 * An implementation of {@link ArgumentResolver} which resolves the {@link TypedValue} of a parameter
 *
 * @param <T> the type of the argument to be resolved
 * @since 4.0
 */
public final class TypedValueArgumentResolver<T> implements ArgumentResolver<TypedValue<T>> {

  private final ByParameterNameArgumentResolver<T> argumentResolver;

  public TypedValueArgumentResolver(String parameterName) {
    argumentResolver = new ByParameterNameArgumentResolver<>(parameterName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TypedValue<T> resolve(ExecutionContext executionContext) {
    T value = argumentResolver.resolve(executionContext);
    return value instanceof TypedValue
        ? (TypedValue<T>) value
        : new TypedValue<>(value, DataType.fromObject(value));
  }
}
