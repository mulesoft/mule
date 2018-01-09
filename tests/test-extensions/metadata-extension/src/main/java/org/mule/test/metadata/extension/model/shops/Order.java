/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.model.shops;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

public class Order {

  @Parameter
  private int price;

  @ParameterGroup(name = "dessert")
  private Dessert dessert;

  public Dessert getDessert() {
    return dessert;
  }

  public int getPrice() {
    return price;
  }
}
