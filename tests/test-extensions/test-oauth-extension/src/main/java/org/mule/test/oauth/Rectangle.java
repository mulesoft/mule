/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.subtypes.extension.ParentShape;

import java.util.Objects;

public class Rectangle extends ParentShape {

  private Integer base;

  private Integer height;

  public Rectangle() {}

  public Rectangle(Integer area, Integer base, Integer height) {
    super(area);
    this.base = base;
    this.height = height;
  }

  public Integer getBase() {
    return base;
  }

  public Integer getHeight() {
    return height;
  }

  public void setBase(Integer base) {
    this.base = base;
  }

  public void setHeight(Integer height) {
    this.height = height;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    Rectangle rectangle = (Rectangle) o;
    return Objects.equals(base, rectangle.base) && Objects.equals(height, rectangle.height);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), base, height);
  }
}
