/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension;

import java.util.List;
import java.util.Map;

public class ComplexActingParameter {

  private int intParam;
  private String stringParam;
  private List<String> listParam;
  private Map<String, String> mapParam;

  private InnerPojo innerPojoParam;
  private List<InnerPojo> complexListParam;
  private Map<String, InnerPojo> complexMapParam;

  public int getIntParam() {
    return intParam;
  }

  public String getStringParam() {
    return stringParam;
  }

  public List<String> getListParam() {
    return listParam;
  }

  public InnerPojo getInnerPojoParam() {
    return innerPojoParam;
  }

  public Map<String, String> getMapParam() {
    return mapParam;
  }

  public List<InnerPojo> getComplexListParam() {
    return complexListParam;
  }

  public Map<String, InnerPojo> getComplexMapParam() {
    return complexMapParam;
  }
}
