/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model.types;

import java.io.Serializable;

public class IntegerAttributes implements Serializable {

  private int value;

  public IntegerAttributes(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

}
