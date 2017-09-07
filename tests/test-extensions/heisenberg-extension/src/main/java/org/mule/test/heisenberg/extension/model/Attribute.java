/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class Attribute {

  @Parameter
  @Alias("attributeName")
  private String name;

  @Parameter
  private String whereValue;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getWhereValue() {
    return whereValue;
  }

  public void setWhereValue(String whereValue) {
    this.whereValue = whereValue;
  }
}
