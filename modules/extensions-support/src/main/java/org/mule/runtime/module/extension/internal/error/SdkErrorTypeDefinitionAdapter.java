/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.error;


import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.sdk.api.error.ErrorTypeDefinition;

import java.util.Objects;
import java.util.Optional;

/**
 * Adapts a legacy {@link org.mule.runtime.extension.api.error.ErrorTypeDefinition} into an sdk-api {@link ErrorTypeDefinition}
 *
 * @param <E> the definition's generic type
 * @since 4.5.0
 */
public class SdkErrorTypeDefinitionAdapter<E extends Enum<E>> implements ErrorTypeDefinition<E> {

  /**
   * Returns an adapted version of the {@code value}.
   *
   * If the {@code value} doesn't need adapting because it's already an {@link ErrorTypeDefinition}, then the same instance is
   * returned
   *
   * @param value the value to adapt
   * @param <E>   the definition's generic type
   * @return an adapted value or the same instance if no adaptation needed
   * @throws IllegalArgumentException if the value is not an instance of an adaptable type
   */
  public static <E extends Enum<E>> ErrorTypeDefinition<E> from(Object value) {
    checkArgument(value != null, "Cannot adapt null value");
    if (value instanceof ErrorTypeDefinition) {
      return (ErrorTypeDefinition<E>) value;
    } else if (value instanceof org.mule.runtime.extension.api.error.ErrorTypeDefinition) {
      return new SdkErrorTypeDefinitionAdapter<E>((org.mule.runtime.extension.api.error.ErrorTypeDefinition) value);
    } else {
      throw new IllegalArgumentException(format("Value of class '%s' is neither a '%s' nor a '%s'",
                                                value.getClass().getName(),
                                                ErrorTypeDefinition.class.getName(),
                                                org.mule.runtime.extension.api.error.ErrorTypeDefinition.class.getName()));
    }
  }

  private final org.mule.runtime.extension.api.error.ErrorTypeDefinition<E> delegate;

  SdkErrorTypeDefinitionAdapter(org.mule.runtime.extension.api.error.ErrorTypeDefinition<E> delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getType() {
    return delegate.getType();
  }

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return delegate.getParent().map(SdkErrorTypeDefinitionAdapter::new);
  }

  public org.mule.runtime.extension.api.error.ErrorTypeDefinition<E> getDelegate() {
    return delegate;
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SdkErrorTypeDefinitionAdapter<?> that = (SdkErrorTypeDefinitionAdapter<?>) o;
    return delegate == that.delegate;
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }
}
