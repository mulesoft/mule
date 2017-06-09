/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.Objects;

public class HankSchrader {

  @Parameter
  @Optional(defaultValue = "true")
  private boolean worksAtDEA;

  @Parameter
  private boolean lovesMinerals;

  public boolean isWorksAtDEA() {
    return worksAtDEA;
  }

  public void setWorksAtDEA(boolean worksAtDEA) {
    this.worksAtDEA = worksAtDEA;
  }

  public boolean isLovesMinerals() {
    return lovesMinerals;
  }

  public void setLovesMinerals(boolean lovesMinerals) {
    this.lovesMinerals = lovesMinerals;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof HankSchrader) {
      HankSchrader otherHank = (HankSchrader) other;
      return worksAtDEA == otherHank.worksAtDEA && lovesMinerals == otherHank.lovesMinerals;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(worksAtDEA, lovesMinerals);
  }
}
