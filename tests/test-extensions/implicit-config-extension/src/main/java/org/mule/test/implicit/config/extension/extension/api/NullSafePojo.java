/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.implicit.config.extension.extension.api;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.Objects;

public class NullSafePojo {

  @Parameter
  @Optional(defaultValue = "5")
  private Integer nullSafeInteger;

  public Integer getNullSafeInteger() {
    return nullSafeInteger;
  }

  public void setNullSafeInteger(Integer nullSafeInteger) {
    this.nullSafeInteger = nullSafeInteger;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    NullSafePojo that = (NullSafePojo) o;
    return Objects.equals(nullSafeInteger, that.nullSafeInteger);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nullSafeInteger);
  }
}
