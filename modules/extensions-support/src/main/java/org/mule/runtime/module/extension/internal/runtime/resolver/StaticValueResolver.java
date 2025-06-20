/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;

import java.util.Optional;

/**
 * A {@link ValueResolver} which always returns the same constant value.
 *
 * @since 3.7.0
 */
public class StaticValueResolver<T> implements ValueResolver<T> {

  private final T value;

  /**
   * Creates a new instance considering that the {@code value} could have some kind of wrapper.
   * <p>
   * Current implementation only considers the {@link Optional}, using {@code orElse(null)} as the mapping function.
   *
   * @param value a possibly wrapped value
   * @param <T>   the value's generic type
   * @return a new {@link StaticValueResolver} pointing to the unwrapped value
   * @since 4.5.0
   */
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

  @Override
  public String toString() {
    return "StaticValueResolver: " + value;
  }
}
