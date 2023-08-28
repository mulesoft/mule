/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.Alias;

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
