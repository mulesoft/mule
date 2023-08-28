/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.subtypes.extension;


import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;

import java.util.Objects;

@TypeDsl(allowTopLevelDefinition = true)
public class Square extends ParentShape {

  private Integer side;

  public Integer getSide() {
    return side;
  }

  public void setSide(Integer side) {
    this.side = side;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    Square square = (Square) o;
    return Objects.equals(side, square.side);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), side);
  }
}
