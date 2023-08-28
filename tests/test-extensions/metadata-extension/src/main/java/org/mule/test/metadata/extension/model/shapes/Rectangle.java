/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.model.shapes;

import java.util.Objects;

public class Rectangle extends Shape {

  private int height;
  private int width;

  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public int getWidth() {
    return width;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Rectangle rectangle = (Rectangle) o;
    return height == rectangle.height &&
        width == rectangle.width;
  }

  @Override
  public int hashCode() {
    return Objects.hash(height, width);
  }
}
