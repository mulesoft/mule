/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;

public abstract class EasyToEatConfig {

  @Parameter
  @Optional(defaultValue = "10")
  private Integer timeToPeel;

  @Parameter
  @NullSafe
  @Optional
  private List<String> mainProducers;

  @Parameter
  @Optional
  @Alias("sample")
  private HealthyFood sampleCustomName;

  @Parameter
  @Optional
  private String shouldNotOverride;

  public String getShouldNotOverride() {
    return shouldNotOverride;
  }

  public void setShouldNotOverride(String shouldNotOverride) {
    this.shouldNotOverride = shouldNotOverride;
  }

  public HealthyFood getSampleCustomName() {
    return sampleCustomName;
  }

  public void setSampleCustomName(HealthyFood sample) {
    this.sampleCustomName = sample;
  }

  public int getTimeToPeel() {
    return timeToPeel;
  }

  public void setTimeToPeel(int timeToPeel) {
    this.timeToPeel = timeToPeel;
  }

  public List<String> getMainProducers() {
    return mainProducers;
  }

  public void setMainProducers(List<String> mainProducers) {
    this.mainProducers = mainProducers;
  }
}
