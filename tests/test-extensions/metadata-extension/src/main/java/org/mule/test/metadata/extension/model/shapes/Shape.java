/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.model.shapes;

public abstract class Shape {

  private String color;

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }
}
