/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import java.util.List;
import java.util.Map;

public class SpreadVeganismOperation {

  public static final String ARGUMENTS_TAB = "Arguments";

  @MediaType(TEXT_PLAIN)
  public String spreadTheWord(String theWord, @Config Object config) {
    return theWord;
  }

  public VeganPolicy applyPolicy(@Optional @NullSafe VeganPolicy policy) {
    return policy;
  }

  @OutputResolver(output = FruitMetadataResolver.class)
  public FarmedFood getProduction(@Optional @NullSafe(defaultImplementingType = HealthyFood.class) FarmedFood food) {
    return food;
  }

  @OutputResolver(output = FruitMetadataResolver.class)
  public FarmedFood getHealthyFood(HealthyFood healthyFood) {
    return healthyFood;
  }

  public Map<String, String> addVeganProductsDescriptions(@Optional @NullSafe Map<String, String> productDescription) {
    return productDescription;
  }

  public List<String> registerVeganProducts(@Optional @NullSafe List<String> products) {
    return products;
  }

  public void convinceAnimalKiller(@ParameterGroup(name = "arguments") @Placement(tab = ARGUMENTS_TAB) VeganArguments arguments) {

  }

  @ExclusiveOptionals
  public static class VeganArguments {

    @Parameter
    @Optional
    private String argument1;

    @Parameter
    @Optional
    private String argument2;

    @Parameter
    @Optional
    private String argument3;

    public String getArgument1() {
      return argument1;
    }

    public String getArgument2() {
      return argument2;
    }

    public String getArgument3() {
      return argument3;
    }
  }
}
