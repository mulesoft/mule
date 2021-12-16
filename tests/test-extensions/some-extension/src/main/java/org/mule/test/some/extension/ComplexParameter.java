/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.some.extension;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.Objects;

public class ComplexParameter {

  @Parameter
  private String anotherParameter;

  @Parameter
  @Optional
  private String yetAnotherParameter;

  @Parameter
  @Optional
  private String repeatedNameParameter;

  public String getAnotherParameter() {
    return anotherParameter;
  }

  public String getYetAnotherParameter() {
    return yetAnotherParameter;
  }

  public String getRepeatedNameParameter() {
    return repeatedNameParameter;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ComplexParameter that = (ComplexParameter) o;
    return Objects.equals(anotherParameter, that.anotherParameter) &&
        Objects.equals(yetAnotherParameter, that.yetAnotherParameter) &&
        Objects.equals(repeatedNameParameter, that.repeatedNameParameter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(anotherParameter, yetAnotherParameter, repeatedNameParameter);
  }
}
