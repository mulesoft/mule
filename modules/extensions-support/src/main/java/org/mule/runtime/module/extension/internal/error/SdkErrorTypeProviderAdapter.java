/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.error;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.sdk.api.annotation.error.ErrorTypeProvider;
import org.mule.sdk.api.error.ErrorTypeDefinition;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adapts a legacy {@link org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider} into an sdk-api
 * {@link ErrorTypeProvider}
 *
 * @since 4.5.0
 */
public class SdkErrorTypeProviderAdapter implements ErrorTypeProvider {

  /**
   * Returns an adapted version of the {@code value}.
   *
   * If the {@code value} doesn't need adapting because it's already an {@link ErrorTypeProvider}, then the same instance is
   * returned
   *
   * @param value the value to adapt
   * @return an adapted value or the same instance if no adaptation needed
   * @throws IllegalArgumentException if the value is not an instance of an adaptable type
   */
  public static ErrorTypeProvider from(Object value) {
    checkArgument(value != null, "Cannot adapt null value");

    if (value instanceof ErrorTypeProvider) {
      return (ErrorTypeProvider) value;
    } else if (value instanceof org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider) {
      return new SdkErrorTypeProviderAdapter((org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider) value);
    } else {
      throw new IllegalArgumentException(String.format("Value of class '%s' is neither a '%s' nor a '%s'",
                                                       value.getClass().getName(),
                                                       ErrorTypeProvider.class.getName(),
                                                       org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider.class
                                                           .getName()));
    }
  }

  private final org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider delegate;

  private SdkErrorTypeProviderAdapter(org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider delegate) {
    this.delegate = delegate;
  }

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    Set<ErrorTypeDefinition> errors = new LinkedHashSet<>();
    for (org.mule.runtime.extension.api.error.ErrorTypeDefinition def : delegate.getErrorTypes()) {
      errors.add(new SdkErrorTypeDefinitionAdapter(def));
    }

    return errors;
  }
}
