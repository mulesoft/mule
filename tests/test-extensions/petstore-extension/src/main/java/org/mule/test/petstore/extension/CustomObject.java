/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@Alias("pojo")
public class CustomObject {

  @Parameter
  private String stringParam;

  @Parameter
  @Optional
  private Object customParam;

  public CustomObject(String stringParam, Object customParam) {
    this.stringParam = stringParam;
    this.customParam = customParam;
  }

  public String getStringParam() {
    return stringParam;
  }

  public Object getCustomParam() {
    return customParam;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    CustomObject that = (CustomObject) o;

    if (stringParam != null ? !stringParam.equals(that.stringParam) : that.stringParam != null)
      return false;
    return customParam != null ? customParam.equals(that.customParam) : that.customParam == null;
  }

  @Override
  public int hashCode() {
    int result = stringParam != null ? stringParam.hashCode() : 0;
    result = 31 * result + (customParam != null ? customParam.hashCode() : 0);
    return result;
  }
}
