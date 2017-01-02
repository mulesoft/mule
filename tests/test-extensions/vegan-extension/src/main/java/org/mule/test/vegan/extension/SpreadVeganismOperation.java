/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

public class SpreadVeganismOperation {

  public static final String ARGUMENTS_TAB = "Arguments";

  public String spreadTheWord(String theWord, @UseConfig Object config) {
    return theWord;
  }

  public VeganPolicy applyPolicy(@Optional @NullSafe VeganPolicy policy) {
    return policy;
  }

  public FarmedFood getProduction(@Optional @NullSafe(defaultImplementingType = HealthyFood.class) FarmedFood food) {
    return food;
  }

  public FarmedFood getHealthyFood(HealthyFood food) {
    return food;
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
