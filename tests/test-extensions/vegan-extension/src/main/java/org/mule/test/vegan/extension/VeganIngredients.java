/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@ExclusiveOptionals
public class VeganIngredients {

  @Parameter
  @Optional
  private int saltMiligrams;

  @Parameter
  @Optional
  private String saltReplacementName;

  public int getSaltMiligrams() {
    return saltMiligrams;
  }

  public String getSaltReplacementName() {
    return saltReplacementName;
  }
}
