/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import java.io.Serializable;
import java.util.List;

public class HarvestApplesAttributes implements Serializable {

  private List<Object> parameters;

  public HarvestApplesAttributes(List<Object> parameters) {
    this.parameters = parameters;
  }

  public List<Object> getParameters() {
    return parameters;
  }
}
