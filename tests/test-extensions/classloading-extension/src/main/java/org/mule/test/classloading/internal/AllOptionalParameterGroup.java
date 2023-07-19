/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.classloading.internal;

import static org.mule.test.classloading.api.ClassLoadingHelper.addClassLoader;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class AllOptionalParameterGroup {

  public static final String ALL_OPTIONAL_PARAMETER_GROUP = "allOptionalParameterGroup";

  @Parameter
  @Optional
  private String optionalString;

  @Parameter
  @Optional
  private Integer optionalInt;

  public AllOptionalParameterGroup() {
    addClassLoader(ALL_OPTIONAL_PARAMETER_GROUP);
  }

  public String getOptionalString() {
    return optionalString;
  }

  public Integer getOptionalInt() {
    return optionalInt;
  }
}
