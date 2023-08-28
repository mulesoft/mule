/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class HarvestInputGroup {

  @Parameter
  @ConfigOverride
  private HealthyFood sample;

  public HealthyFood getSample() {
    return sample;
  }

  public void setSample(HealthyFood sample) {
    this.sample = sample;
  }
}
