/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.Objects;

@TypeDsl(allowTopLevelDefinition = true)
public class GroupedFood {

  @Parameter
  @Optional
  @NullSafe(defaultImplementingType = RottenFood.class)
  private FarmedFood food;

  public FarmedFood getFood() {
    return food;
  }

  public void setFood(FarmedFood food) {
    this.food = food;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GroupedFood that = (GroupedFood) o;
    return Objects.equals(food, that.food);
  }

  @Override
  public int hashCode() {
    return Objects.hash(food);
  }
}
