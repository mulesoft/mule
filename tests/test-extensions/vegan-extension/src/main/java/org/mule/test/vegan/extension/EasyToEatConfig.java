/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;

public abstract class EasyToEatConfig {

  public enum Time {
    SHORT, AVERAGE, LONG;
  }

  @Parameter
  @Optional(defaultValue = "10")
  private Integer timeToPeel;

  @Parameter
  @Optional(defaultValue = "AVERAGE")
  private Time timeToPeelEnum;

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
