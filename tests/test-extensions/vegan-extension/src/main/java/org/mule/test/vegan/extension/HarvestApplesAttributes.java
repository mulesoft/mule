/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
