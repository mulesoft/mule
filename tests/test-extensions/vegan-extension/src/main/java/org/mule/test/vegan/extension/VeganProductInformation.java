/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class VeganProductInformation {

  @Parameter
  @Content(primary = true)
  private TypedValue<String> description;

  @Parameter
  @Optional
  @Content
  private TypedValue<String> brandName;

  @Optional
  @Parameter
  private TypedValue<Integer> weight;

  public TypedValue<String> getDescription() {
    return description;
  }

  public void setDescription(TypedValue<String> description) {
    this.description = description;
  }

  public TypedValue<String> getBrandName() {
    return brandName;
  }

  public void setBrandName(TypedValue<String> brandName) {
    this.brandName = brandName;
  }

  public TypedValue<Integer> getWeight() {
    return weight;
  }

  public void setWeight(TypedValue<Integer> weight) {
    this.weight = weight;
  }
}
