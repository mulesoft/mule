/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension;

import java.util.List;
import java.util.Map;

public class InnerPojo {

  private int intParam;
  private String stringParam;
  private List<String> listParam;
  private Map<String, String> mapParam;

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
