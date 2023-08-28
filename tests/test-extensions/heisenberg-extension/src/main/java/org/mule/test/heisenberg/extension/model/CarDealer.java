/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.param.Parameter;

public class CarDealer extends Investment {

  @Parameter
  private int carStock;

  public int getCarStock() {
    return carStock;
  }
}
