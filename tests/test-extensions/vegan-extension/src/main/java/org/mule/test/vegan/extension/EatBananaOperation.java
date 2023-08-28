/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
