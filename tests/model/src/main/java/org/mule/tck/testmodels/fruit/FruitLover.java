/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.testmodels.fruit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FruitLover {

  private final List<Class<? extends Fruit>> eatList = Collections.synchronizedList(new ArrayList<Class<? extends Fruit>>());
  private final String catchphrase;

  public FruitLover(String catchphrase) {
    this.catchphrase = catchphrase;
  }

  public void eatFruit(Fruit fruit) {
    fruit.bite();
    eatList.add(fruit.getClass());
  }

  public List<Class<? extends Fruit>> getEatList() {
    return eatList;
  }

  public String speak() {
    return catchphrase;
  }
}
