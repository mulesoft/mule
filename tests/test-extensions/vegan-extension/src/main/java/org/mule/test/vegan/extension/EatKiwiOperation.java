/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.tck.testmodels.fruit.WaterMelon;

public class EatKiwiOperation {

  public Kiwi eatKiwi(@org.mule.sdk.api.annotation.param.Connection Kiwi kiwi, @Config KiwiConfig config) {
    kiwi.bite();
    return kiwi;
  }

  public WaterMelon eatWatermelon(WaterMelon fruit) {
    throw new IllegalArgumentException("Unlike popular belief, a Kiwi is not a small water melon");
  }
}
