/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Optional.ofNullable;
import org.mule.runtime.extension.api.runtime.parameter.Literal;

import java.util.Optional;

/**
 * Immutable implementation of {@link Literal}
 *
 * @param <T> the generic type of the actual value represented by the literal
 * @since 4.0
 */
public final class ImmutableLiteral<T> implements Literal<T> {

  private final Optional<String> value;
  private final Class<T> type;

  /**
   * Creates a new instance
   *
   * @param value the literal value
   * @param type the type of the value the literal represents
   */
  public ImmutableLiteral(String value, Class<T> type) {
    this.value = ofNullable(value);
    this.type = type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<String> getLiteralValue() {
    return value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<T> getType() {
    return type;
  }
}
