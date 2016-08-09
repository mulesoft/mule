/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
