/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
