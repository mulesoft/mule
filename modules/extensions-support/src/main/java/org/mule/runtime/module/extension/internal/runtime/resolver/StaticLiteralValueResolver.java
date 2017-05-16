/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.runtime.parameter.Literal;

/**
 * {@link ValueResolver} that produces instances of {@link Literal}
 *
 * @param <T> the generic type of the value represented by the literal
 * @since 4.0
 */
public class StaticLiteralValueResolver<T> implements ValueResolver<Literal<T>> {

  private final Literal<T> literal;

  /**
   * Creates a new instance
   *
   * @param value the literal value
   * @param type  the type of the value represented by the literal
   */
  public StaticLiteralValueResolver(String value, Class<T> type) {
    literal = new ImmutableLiteral<>(value, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Literal<T> resolve(ValueResolvingContext context) throws MuleException {
    return literal;
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return false;
  }
}
