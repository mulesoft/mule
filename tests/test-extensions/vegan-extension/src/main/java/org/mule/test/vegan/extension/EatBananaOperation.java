/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.dsl.xml.ParameterDsl;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.tck.testmodels.fruit.Banana;

public class EatBananaOperation {

  public Banana eatBanana(@Connection Banana banana, @Config BananaConfig config) {
    banana.bite();
    return banana;
  }

  public Banana eatPealed(@ParameterDsl(allowInlineDefinition = false, allowReferences = false) Banana attributeOnlyBanana,
                          @Config BananaConfig config) {
    return eatBanana(attributeOnlyBanana, config);
  }

  public BananaConfig getConfig(@Config BananaConfig config) {
    return config;
  }
}
