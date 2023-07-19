/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.some.extension;

import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@ExclusiveOptionals(isOneRequired = true)
public class SomeParameterGroupOneRequiredConfig {

  @Parameter
  @Optional
  private String someParameter;

  @Parameter
  @Optional
  private String repeatedNameParameter;

  @Parameter
  @Optional
  private ComplexParameter complexParameter;

  public String getSomeParameter() {
    return someParameter;
  }

  public String getRepeatedNameParameter() {
    return repeatedNameParameter;
  }

  public ComplexParameter getComplexParameter() {
    return complexParameter;
  }
}
