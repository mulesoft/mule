/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.subtypes.extension;

import org.mule.runtime.extension.api.annotation.param.Parameter;

public class NoGlobalPojo {

  @Parameter
  private String name;

  @Parameter
  private int number;

  @Parameter
  private String string;

  public int getNumber() {
    return number;
  }

  public String getString() {
    return string;
  }

  public String getName() {
    return name;
  }

}
