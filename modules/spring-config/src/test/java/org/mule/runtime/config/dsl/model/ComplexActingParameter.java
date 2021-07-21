/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComplexActingParameter {

  private int intParam;
  private String stringParam;

  private InnerPojo innerPojoParam;

  private List<String> listParam;
  private Map<String, String> mapParam;

  private List<InnerPojo> complexListParam;
  private Map<String, InnerPojo> complexMapParam;

  public ComplexActingParameter() {}

  public ComplexActingParameter(int intParam,
                                String stringParam,
                                List<String> listParam,
                                Map<String, String> mapParam,
                                InnerPojo innerPojoParam,
                                List<InnerPojo> complexListParam,
                                Map<String, InnerPojo> complexMapParam) {
    this.intParam = intParam;
    this.stringParam = stringParam;
    this.listParam = listParam;
    this.mapParam = mapParam;
    this.innerPojoParam = innerPojoParam;
    this.complexListParam = complexListParam;
    this.complexMapParam = complexMapParam;
  }

  public ComplexActingParameter copy() {
    return new ComplexActingParameter(intParam,
                                      stringParam,
                                      new ArrayList<>(listParam),
                                      new HashMap<>(mapParam),
                                      innerPojoParam.copy(),
                                      new ArrayList<>(complexListParam),
                                      new HashMap<>(complexMapParam));
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

  public ComplexActingParameter setIntParam(int intParam) {
    this.intParam = intParam;
    return this;
  }

  public ComplexActingParameter setStringParam(String stringParam) {
    this.stringParam = stringParam;
    return this;
  }

  public ComplexActingParameter setInnerPojoParam(InnerPojo innerPojoParam) {
    this.innerPojoParam = innerPojoParam;
    return this;
  }

  public ComplexActingParameter setListParam(List<String> listParam) {
    this.listParam = listParam;
    return this;
  }

  public ComplexActingParameter setMapParam(Map<String, String> mapParam) {
    this.mapParam = mapParam;
    return this;
  }

  public ComplexActingParameter setComplexListParam(List<InnerPojo> complexListParam) {
    this.complexListParam = complexListParam;
    return this;
  }

  public ComplexActingParameter setComplexMapParam(Map<String, InnerPojo> complexMapParam) {
    this.complexMapParam = complexMapParam;
    return this;
  }
}
