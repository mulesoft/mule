/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.model.shapes;

public class Circle extends Shape {

  private int id;
  private int diameter;

  public int getDiameter() {
    return diameter;
  }

  public void setDiameter(int diameter) {
    this.diameter = diameter;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }
}
