/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import java.util.List;

public class ComplexActingParameter {

  private int intParam;
  private String stringParam;
  private List<String> listParam;
  private InnerPojo innerPojoParam;

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

  private static class InnerPojo {

    private int intParam;
    private String stringParam;
    private List<String> listParam;

    public int getIntParam() {
      return intParam;
    }

    public String getStringParam() {
      return stringParam;
    }

    public List<String> getListParam() {
      return listParam;
    }
  }

}
