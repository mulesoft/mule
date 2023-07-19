/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
