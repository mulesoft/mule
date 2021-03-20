/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.value;

import static java.lang.String.format;

import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.values.ValueProvider;

/**
 * Encapsulates either a {@link ValueProvider} class or {@link org.mule.sdk.api.values.ValueProvider} class in order to be able to
 * use a single object to represent both classes.
 *
 * If the {@link EitherValueProvider} is used with a class that does not extend {@link ValueProvider} or
 * {@link org.mule.sdk.api.values.ValueProvider}, an {@link IllegalModelDefinitionException} exception is thrown.
 *
 * @since 4.4.0
 */
public class EitherValueProvider {

  private Class valueProvider;

  public EitherValueProvider(Class valueProvider) {
    if (org.mule.sdk.api.values.ValueProvider.class.isAssignableFrom(valueProvider) ||
        ValueProvider.class.isAssignableFrom(valueProvider)) {
      this.valueProvider = valueProvider;
    } else {
      throw new IllegalModelDefinitionException(format("Invalid use of class %s. A value provider class should extend %s or %s",
                                                       valueProvider.getName(),
                                                       ValueProvider.class.getName(),
                                                       org.mule.sdk.api.values.ValueProvider.class.getName()));
    }
  }

  public Class get() {
    return valueProvider;
  }

}
