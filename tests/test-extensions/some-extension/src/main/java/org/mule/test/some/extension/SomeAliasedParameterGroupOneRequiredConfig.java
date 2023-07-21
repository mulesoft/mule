/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.some.extension;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@ExclusiveOptionals(isOneRequired = true)
public class SomeAliasedParameterGroupOneRequiredConfig {

  @Parameter
  @Optional
  @Alias(value = "some-parameter-alias")
  private String aliasedSomeParameter;

  @Parameter
  @Optional
  @Alias(value = "complex-parameter-alias")
  private AliasedComplexParameter aliasedComplexParameter;

  public String getSomeParameter() {
    return aliasedSomeParameter;
  }

  public AliasedComplexParameter getComplexParameter() {
    return aliasedComplexParameter;
  }
}
