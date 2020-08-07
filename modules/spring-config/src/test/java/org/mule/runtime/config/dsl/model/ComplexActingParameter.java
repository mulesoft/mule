/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import java.util.List;
import java.util.Map;

public class ComplexActingParameter {

  private int intParam;
  private String stringParam;
  private InnerPojo innerPojoParam;

  private List<String> listParam;
  private Map<String, String> mapParam;

  private List<InnerPojo> complexListParam;
  //private Map<String, InnerPojo> complexMapParam;

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

  //public Map<String, InnerPojo> getComplexMapParam() {
  //  return complexMapParam;
  //}
}
