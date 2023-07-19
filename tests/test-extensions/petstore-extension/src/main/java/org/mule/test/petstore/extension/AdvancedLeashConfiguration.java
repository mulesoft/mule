/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class AdvancedLeashConfiguration {

  @Parameter
  @Optional
  public String brand;

  @Parameter
  @Optional
  public String material;

  public String getBrand() {
    return brand;
  }

  public String getMaterial() {
    return material;
  }
}
