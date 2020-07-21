/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.api.pojos;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@TypeDsl(allowTopLevelDefinition = true)
public class MyPojo {

  @Parameter
  @Optional(defaultValue = "jose")
  private String someParameter;

  public MyPojo() {}

  public MyPojo(String someParameter) {
    this.someParameter = someParameter;
  }

  public void setSomeParameter(String someParameter) {
    this.someParameter = someParameter;
  }

  public String getSomeParameter() {
    return someParameter;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MyPojo)) {
      return false;
    }

    MyPojo that = (MyPojo) o;

    return someParameter != null ? someParameter.equals(that.someParameter) : that.someParameter == null;

  }

  @Override
  public int hashCode() {
    return someParameter != null ? someParameter.hashCode() : 0;
  }
}
