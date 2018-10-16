/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;


import static org.mule.test.vegan.extension.VeganExtension.APPLE;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.deprecated.Deprecated;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.test.vegan.extension.stereotype.AppleStereotype;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration(name = APPLE)
@Operations({EatAppleOperation.class, SpreadVeganismOperation.class, VeganFidelityOperation.class,
    FruitOperationsWithConfigOverride.class})
@Sources({HarvestApplesSource.class, PaulMcCartneySource.class})
@ConnectionProviders(VeganAppleConnectionProvider.class)
@Stereotype(AppleStereotype.class)
@Deprecated(message = "This configuration overlaps with the BananaConfig, use that one instead.")
public class AppleConfig extends EasyToEatConfig {

  @Parameter
  private VeganCookBook cookBook;

  private Map<String, List<Object>> results = new HashMap<>();

  public VeganCookBook getCookBook() {
    return cookBook;
  }

  public Map<String, List<Object>> getResults() {
    return results;
  }
}
