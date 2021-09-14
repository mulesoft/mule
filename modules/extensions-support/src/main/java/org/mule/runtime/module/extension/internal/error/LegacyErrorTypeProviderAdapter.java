/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.error;

import static java.util.stream.Collectors.toCollection;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.sdk.api.annotation.error.ErrorTypeProvider;
import org.mule.sdk.api.error.ErrorTypeDefinition;

import java.util.LinkedHashSet;
import java.util.Set;

public class LegacyErrorTypeProviderAdapter implements ErrorTypeProvider {

  public static ErrorTypeProvider from(Object value) {
    checkArgument(value != null, "Cannot adapt null value");

    if (value instanceof ErrorTypeProvider) {
      return (ErrorTypeProvider) value;
    } else if (value instanceof org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider) {
      return new LegacyErrorTypeProviderAdapter((org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider) value);
    } else {
      throw new IllegalArgumentException(String.format("Value of class '%s' is neither a '%s' or a '%s'",
                                                       value.getClass().getName(),
                                                       ErrorTypeProvider.class.getName(),
                                                       org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider.class
                                                           .getName()));
    }
  }

  private final org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider delegate;

  public LegacyErrorTypeProviderAdapter(org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider delegate) {
    this.delegate = delegate;
  }

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return (Set<ErrorTypeDefinition>) delegate.getErrorTypes().stream()
        .map(LegacyErrorTypeDefinitionAdapter::new)
        .collect(toCollection(LinkedHashSet::new));
  }
}
