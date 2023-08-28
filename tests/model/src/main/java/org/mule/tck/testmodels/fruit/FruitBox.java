/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.testmodels.fruit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FruitBox {

  private List<? extends Fruit> fruitLikeList = new ArrayList<>();
  private List<Fruit> fruitList = new ArrayList<>();
  private List<?> wildCardList = new ArrayList<>();
  private List rawList = new ArrayList<>();

  private Map<?, ?> wildCardMap = new HashMap<>();
  private Map<?, ? extends Fruit> fruitLikeMap = new HashMap<>();
  private Map rawMap = new HashMap<>();

  public List<? extends Fruit> getFruitLikeList() {
    return fruitLikeList;
  }

  public List<?> getWildCardList() {
    return wildCardList;
  }

  public List getRawList() {
    return rawList;
  }

  public Map<?, ?> getWildCardMap() {
    return wildCardMap;
  }

  public Map<?, ? extends Fruit> getFruitLikeMap() {
    return fruitLikeMap;
  }

  public Map getRawMap() {
    return rawMap;
  }

}
