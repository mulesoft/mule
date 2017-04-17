/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.tck.testmodels.fruit.WaterMelon;

public class EatKiwiOperation {

  public Kiwi eatKiwi(@Connection Kiwi kiwi, @Config KiwiConfig config) {
    kiwi.bite();
    return kiwi;
  }

  public WaterMelon eatWatermelon(WaterMelon fruit) {
    throw new IllegalArgumentException("Unlike popular belief, a Kiwi is not a small water melon");
  }
}
