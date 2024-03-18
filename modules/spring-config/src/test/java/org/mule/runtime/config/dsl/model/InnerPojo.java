/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InnerPojo {

  private int intParam;
  private String stringParam;
  private List<String> listParam;
  private Map<String, String> mapParam;

  public InnerPojo() {}

  public InnerPojo(int intParam, String stringParam, List<String> listParam, Map<String, String> mapParam) {
    this.intParam = intParam;
    this.stringParam = stringParam;
    this.listParam = listParam;
    this.mapParam = mapParam;
  }

  public InnerPojo copy() {
    return new InnerPojo(intParam, stringParam, new ArrayList<>(listParam), new HashMap<>(mapParam));
  }

  public int getIntParam() {
    return intParam;
  }

  public String getStringParam() {
    return stringParam;
  }

  public List<String> getListParam() {
    return listParam;
  }

  public Map<String, String> getMapParam() {
    return mapParam;
  }

}
