/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
