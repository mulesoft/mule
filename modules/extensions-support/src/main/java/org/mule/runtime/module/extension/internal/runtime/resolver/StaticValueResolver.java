/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.exception.MuleException;

import java.util.Optional;

/**
 * A {@link ValueResolver} which always returns the same constant value.
 *
 * @since 3.7.0
 */
public class StaticValueResolver<T> implements ValueResolver<T> {

  private final T value;

  public static <T> StaticValueResolver<T> fromUnwrapped(Object value) {
    if (value instanceof Optional) {
      value = ((Optional<?>) value).orElse(null);
    }

    return new StaticValueResolver<>((T) value);
  }

  public StaticValueResolver(T value) {
    this.value = value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T resolve(ValueResolvingContext context) throws MuleException {
    return value;
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return false;
  }
}
