/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

  public void setFruitLikeList(List<? extends Fruit> fruitLikeList) {
    this.fruitLikeList = fruitLikeList;
  }

  public void setFruitList(List<Fruit> fruitList) {
    this.fruitList = fruitList;
  }

  public void setWildCardList(List<?> wildCardList) {
    this.wildCardList = wildCardList;
  }

  public void setRawList(List rawList) {
    this.rawList = rawList;
  }

  public void setWildCardMap(Map<?, ?> wildCardMap) {
    this.wildCardMap = wildCardMap;
  }

  public void setFruitLikeMap(Map<?, ? extends Fruit> fruitLikeMap) {
    this.fruitLikeMap = fruitLikeMap;
  }

  public void setRawMap(Map rawMap) {
    this.rawMap = rawMap;
  }

}
