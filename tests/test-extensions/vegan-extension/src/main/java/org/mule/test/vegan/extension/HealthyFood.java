/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.Objects;

@TypeDsl(allowTopLevelDefinition = true)
public class HealthyFood implements FarmedFood {

  @Parameter
  @NullSafe
  @Optional
  private TasteProfile tasteProfile;

  @Override
  public boolean canBeEaten() {
    return true;
  }

  public TasteProfile getTasteProfile() {
    return tasteProfile;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    HealthyFood that = (HealthyFood) o;
    return Objects.equals(tasteProfile, that.tasteProfile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tasteProfile);
  }

  public static class TasteProfile {

    @Parameter
    @Optional(defaultValue = "false")
    private boolean tasty;

    public boolean isTasty() {
      return tasty;
    }

  }
}
