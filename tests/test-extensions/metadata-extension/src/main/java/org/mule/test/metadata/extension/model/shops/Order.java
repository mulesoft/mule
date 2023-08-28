/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.model.shops;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Order order = (Order) o;
    return price == order.price &&
        Objects.equals(dessert, order.dessert);
  }

  @Override
  public int hashCode() {
    return Objects.hash(price, dessert);
  }
}
