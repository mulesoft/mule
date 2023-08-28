/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@TypeDsl(allowTopLevelDefinition = true)
public class CarWash extends Investment {

  @Parameter
  private int carsPerMinute;

  public int getCarsPerMinute() {
    return carsPerMinute;
  }
}
