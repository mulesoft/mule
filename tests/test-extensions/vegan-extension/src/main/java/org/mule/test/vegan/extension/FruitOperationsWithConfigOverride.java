/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.test.vegan.extension.EasyToEatConfig.Time;

import java.util.List;

public class FruitOperationsWithConfigOverride {

  public Integer getTimeToPeel(@Config EasyToEatConfig config,
                               @ConfigOverride Integer timeToPeel) {
    return timeToPeel;
  }

  public long getTimeToPeelLong(@org.mule.sdk.api.annotation.param.Config EasyToEatConfig config,
                                @ConfigOverride long timeToPeel) {
    return timeToPeel;
  }

  public Time getTimeToPeelEnum(@Config EasyToEatConfig config,
                                @ConfigOverride Time timeToPeelEnum) {
    return timeToPeelEnum;
  }

  public List<String> getProducers(@org.mule.sdk.api.annotation.param.Config EasyToEatConfig config,
                                   @ConfigOverride List<String> mainProducers) {
    return mainProducers;
  }

  public HealthyFood getFruitSample(@Config EasyToEatConfig config,
                                    @ConfigOverride HealthyFood sample) {
    return sample;
  }

  @MediaType(TEXT_PLAIN)
  public String getNotOverridenParameter(@Config EasyToEatConfig config,
                                         @Optional String shouldNotOverride) {
    return shouldNotOverride;
  }

}
