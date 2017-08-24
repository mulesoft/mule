/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.exception.MuleException;

/**
 * An {@link AbstractValueResolverWrapper} which throws an {@link IllegalArgumentException} if the resolved
 * value is {@code null}.
 * <p>
 * This wrapper is intended to be used on parameters which have been assigned to an expression, but we want to prevent
 * that expression from evaluating to {@code null}
 *
 * @param <T> the generic type of the resolved values
 * @since 4.0
 */
public class RequiredExpressionParameterValueResolverWrapper<T> extends AbstractValueResolverWrapper<T> {

  private final String parameterName;
  private final String expression;

  /**
   * Creates a new instance
   *
   * @param delegate      the wrapped {@link ValueResolver}
   * @param parameterName the name of the parameter this resolver is associated to
   * @param expression    the evaluated expression
   */
  public RequiredExpressionParameterValueResolverWrapper(ValueResolver<T> delegate,
                                                         String parameterName,
                                                         String expression) {
    super(delegate);
    this.parameterName = parameterName;
    this.expression = expression;
  }

  @Override
  public T resolve(ValueResolvingContext context) throws MuleException {
    T value = super.resolve(context);
    if (value == null) {
      throw new IllegalArgumentException(String.format(
                                                       "Required parameter '%s' was assigned with expression '%s' which resolved to null. Required parameters need to be "
                                                           + "assigned with non null values",
                                                       parameterName, expression));
    }

    return value;
  }
}
