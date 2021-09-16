/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.error;


import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.sdk.api.error.ErrorTypeDefinition;

import java.util.Objects;
import java.util.Optional;

public class LegacyErrorTypeDefinitionAdapter<E extends Enum<E>> implements ErrorTypeDefinition<E> {

  public static <E extends Enum<E>> ErrorTypeDefinition<E> from(Object value) {
    checkArgument(value != null, "Cannot adapt null value");
    if (value instanceof ErrorTypeDefinition) {
      return (ErrorTypeDefinition<E>) value;
    } else if (value instanceof org.mule.runtime.extension.api.error.ErrorTypeDefinition) {
      return new LegacyErrorTypeDefinitionAdapter<E>((org.mule.runtime.extension.api.error.ErrorTypeDefinition) value);
    } else {
      throw new IllegalArgumentException(String.format("Value of class '%s' is neither a '%s' or a '%s'",
                                                       value.getClass().getName(),
                                                       ErrorTypeDefinition.class.getName(),
                                                       org.mule.runtime.extension.api.error.ErrorTypeDefinition.class.getName()));
    }
  }

  private final org.mule.runtime.extension.api.error.ErrorTypeDefinition<E> delegate;

  public LegacyErrorTypeDefinitionAdapter(org.mule.runtime.extension.api.error.ErrorTypeDefinition<E> delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getType() {
    return delegate.getType();
  }

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return delegate.getParent().map(LegacyErrorTypeDefinitionAdapter::new);
  }

  public org.mule.runtime.extension.api.error.ErrorTypeDefinition<E> getDelegate() {
    return delegate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LegacyErrorTypeDefinitionAdapter<?> that = (LegacyErrorTypeDefinitionAdapter<?>) o;
    return delegate == that.delegate;
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }
}
