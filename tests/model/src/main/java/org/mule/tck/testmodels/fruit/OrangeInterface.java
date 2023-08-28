/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tck.testmodels.fruit;

import java.util.List;
import java.util.Map;

/**
 * TODO
 */

public interface OrangeInterface extends Fruit {

  String getBrand();

  Integer getSegments();

  Double getRadius();

  void setBrand(String string);

  void setSegments(Integer integer);

  void setRadius(Double double1);

  List getListProperties();

  void setListProperties(List listProperties);

  Map getMapProperties();

  void setMapProperties(Map mapProperties);

  List getArrayProperties();

  void setArrayProperties(List arrayProperties);

  FruitCleaner getCleaner();

  void setCleaner(FruitCleaner cleaner);

  void wash();

  void polish();
}
