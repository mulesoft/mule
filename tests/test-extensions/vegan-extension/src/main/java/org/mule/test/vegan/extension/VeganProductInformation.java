/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
