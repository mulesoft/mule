/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO
 */
public class FruitBasket {

  private final Map<Class<? extends Fruit>, Fruit> basket =
      Collections.synchronizedMap(new HashMap<Class<? extends Fruit>, Fruit>());

  public boolean hasApple() {
    return basket.get(Apple.class) != null;
  }

  public boolean hasBanana() {
    return basket.get(Banana.class) != null;
  }

  public void setFruit(Fruit[] fruit) {
    for (int i = 0; i < fruit.length; i++) {
      basket.put(fruit[i].getClass(), fruit[i]);
    }
  }

  public void setFruit(List<Fruit> fruit) {
    this.setFruit(fruit.toArray(new Fruit[fruit.size()]));
  }

  public List<Fruit> getFruit() {
    return new ArrayList<Fruit>(basket.values());
  }

  public Apple getApple() {
    return (Apple) basket.get(Apple.class);
  }

  public Banana getBanana() {
    return (Banana) basket.get(Banana.class);
  }
}
