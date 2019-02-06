/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

import java.util.Objects;

public class VeganPolicy {

  @Parameter
  @Optional(defaultValue = "false")
  private boolean meetAllowed;

  @ParameterGroup(name = "Ingredients")
  private VeganIngredients ingredients;

  @Parameter
  @Optional(defaultValue = "500")
  private Integer maxCalories;

  public boolean getMeetAllowed() {
    return meetAllowed;
  }

  public VeganIngredients getIngredients() {
    return ingredients;
  }

  public Integer getMaxCalories() {
    return maxCalories;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    VeganPolicy that = (VeganPolicy) o;
    return meetAllowed == that.meetAllowed &&
        Objects.equals(ingredients, that.ingredients) &&
        Objects.equals(maxCalories, that.maxCalories);
  }

  @Override
  public int hashCode() {
    return Objects.hash(meetAllowed, ingredients, maxCalories);
  }
}
