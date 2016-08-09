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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FruitBowl {

  /**
   * logger used by this class
   */
  private static final Logger logger = LoggerFactory.getLogger(FruitBowl.class);

  private final Map<Class<?>, Fruit> bowl = Collections.synchronizedMap(new HashMap<Class<?>, Fruit>());

  public FruitBowl() {
    super();
  }

  public FruitBowl(Fruit fruit[]) {
    for (int i = 0; i < fruit.length; i++) {
      bowl.put(fruit[i].getClass(), fruit[i]);
    }
  }

  public FruitBowl(Apple apple, Banana banana) {
    bowl.put(Apple.class, apple);
    bowl.put(Banana.class, banana);
  }

  public boolean hasApple() {
    return bowl.get(Apple.class) != null;
  }

  public boolean hasBanana() {
    return bowl.get(Banana.class) != null;
  }

  public void addFruit(Fruit fruit) {
    bowl.put(fruit.getClass(), fruit);
  }

  public Fruit[] addAppleAndBanana(Apple apple, Banana banana) {
    bowl.put(Apple.class, apple);
    bowl.put(Banana.class, banana);
    return new Fruit[] {apple, banana};
  }

  public Fruit[] addBananaAndApple(Banana banana, Apple apple) {
    bowl.put(Apple.class, apple);
    bowl.put(Banana.class, banana);
    return new Fruit[] {banana, apple};

  }

  public List<Fruit> getFruit() {
    return new ArrayList<Fruit>(bowl.values());
  }

  public Object consumeFruit(FruitLover fruitlover) {
    logger.debug("Got a fruit lover who says: " + fruitlover.speak());
    for (Fruit fruit : bowl.values()) {
      fruit.bite();
    }
    return fruitlover;
  }

  public void setFruit(Fruit[] fruit) {
    for (int i = 0; i < fruit.length; i++) {
      bowl.put(fruit[i].getClass(), fruit[i]);
    }
  }

  public void setFruit(List<Fruit> fruit) {
    this.setFruit(fruit.toArray(new Fruit[fruit.size()]));
  }

  public Apple getApple() {
    return (Apple) bowl.get(Apple.class);
  }

  public void setApple(Apple apple) {
    bowl.put(Apple.class, apple);
  }

  public Banana getBanana() {
    return (Banana) bowl.get(Banana.class);
  }

  public void setBanana(Banana banana) {
    bowl.put(Banana.class, banana);
  }

}
