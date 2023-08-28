/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.data.sample.extension;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class SampleDataParameterGroup {

  @Parameter
  private String groupParameter;

  @Parameter
  @Optional
  private String optionalParameter;

  public String getGroupParameter() {
    return groupParameter;
  }

  public String getOptionalParameter() {
    return optionalParameter;
  }
}
