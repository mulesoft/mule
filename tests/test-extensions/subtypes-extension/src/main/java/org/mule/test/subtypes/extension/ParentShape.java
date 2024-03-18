/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.subtypes.extension;

import org.mule.sdk.api.annotation.semantics.connectivity.Port;

import java.util.Objects;

public class ParentShape {

  @Port
  private Integer area;

  public Integer getArea() {
    return area;
  }

  public void setArea(Integer area) {
    this.area = area;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ParentShape that = (ParentShape) o;
    return Objects.equals(area, that.area);
  }

  @Override
  public int hashCode() {
    return Objects.hash(area);
  }
}
