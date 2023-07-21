/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.some.extension;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class AliasedComplexParameter {

  @Parameter
  @Alias(value = "another-parameter-alias")
  private String anotherParameter;

  @Parameter
  @Optional
  @Alias(value = "yet-another-parameter-alias")
  private String yetAnotherParameter;

  public String getAnotherParameter() {
    return anotherParameter;
  }

  public String getYetAnotherParameter() {
    return yetAnotherParameter;
  }
}
