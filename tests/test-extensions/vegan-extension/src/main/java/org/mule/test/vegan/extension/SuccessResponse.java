/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class SuccessResponse {

  @Parameter
  @ConfigOverride
  private Integer timeToPeel;

  public Integer getTimeToPeel() {
    return timeToPeel;
  }

  public void setTimeToPeel(Integer timeToPeel) {
    this.timeToPeel = timeToPeel;
  }
}
