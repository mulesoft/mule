/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.parameter.Literal;

/**
 * {@link ArgumentResolver} for parameters which are of {@link Literal} type
 *
 * @param <T> the generic type of the value represented by the literal
 * @since 4.0
 */
public class LiteralArgumentResolver<T> implements ArgumentResolver<Literal<T>> {

  private final Class<T> expectedType;
  private final ByParameterNameArgumentResolver argumentResolver;

  public LiteralArgumentResolver(String parameterName, Class<T> expectedType) {
    argumentResolver = new ByParameterNameArgumentResolver<>(parameterName);
    this.expectedType = expectedType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Literal<T> resolve(ExecutionContext executionContext) {
    Object value = argumentResolver.resolve(executionContext);
    if (value instanceof Literal) {
      return (Literal<T>) value;
    } else if (value == null) {
      return null;
    }

    checkArgument(value instanceof String, "Resolved value was expected to be a String");
    return new ImmutableLiteral<>((String) value, expectedType);
  }
}
