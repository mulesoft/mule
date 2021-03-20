/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.value;

import static java.lang.String.format;

import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.sdk.api.annotation.values.OfValues;

/**
 * Encapsulates either a {@link OfValues} annotation or a {@link org.mule.runtime.extension.api.annotation.values.OfValues}
 * annotation in order to be able to use a single object to represent both.
 * <p>
 * If the {@link EitherOfValue} is used with an annotation other than {@link OfValues} or a
 * {@link org.mule.runtime.extension.api.annotation.values.OfValues}, an {@link IllegalModelDefinitionException} exception is
 * thrown.
 *
 * @since 4.4.0
 */
public class EitherOfValue {

  private final Object ofValue;
  private boolean isLegacyOfValue;

  public EitherOfValue(Object ofValue) {
    if (ofValue instanceof org.mule.runtime.extension.api.annotation.values.OfValues) {
      isLegacyOfValue = true;
    } else if (ofValue instanceof OfValues) {
      isLegacyOfValue = false;
    } else {
      throw new IllegalModelDefinitionException(format("Invalid use of annotation %s. A value provider should use %s or %s",
                                                       ofValue.getClass().getName(),
                                                       OfValues.class.getName(),
                                                       org.mule.runtime.extension.api.annotation.values.OfValues.class
                                                           .getName()));
    }

    this.ofValue = ofValue;
  }

  public Class<?> value() {
    return isLegacyOfValue ? ((org.mule.runtime.extension.api.annotation.values.OfValues) ofValue).value()
        : ((OfValues) ofValue).value();
  }

  public boolean open() {
    return isLegacyOfValue ? ((org.mule.runtime.extension.api.annotation.values.OfValues) ofValue).open()
        : ((OfValues) ofValue).open();
  }
}
