/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.model.shops;

import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@ExclusiveOptionals(isOneRequired = true)
public class Dessert {

  @Parameter
  @Optional
  private String cakeName;

  @Parameter
  @Optional
  private String iceCreamName;

  public String getCakeName() {
    return cakeName;
  }

  public String getIceCreamName() {
    return iceCreamName;
  }
}
