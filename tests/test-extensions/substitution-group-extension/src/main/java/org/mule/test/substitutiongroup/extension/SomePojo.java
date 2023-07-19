/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.substitutiongroup.extension;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.Objects;

@TypeDsl(substitutionGroup = "heisenberg:global-abstract-weapon",
    baseType = "heisenberg:org.mule.test.heisenberg.extension.model.Weapon")
public class SomePojo {

  @Parameter
  private boolean attribute;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    SomePojo somePojo = (SomePojo) o;
    return attribute == somePojo.attribute;
  }

  @Override
  public int hashCode() {
    return Objects.hash(attribute);
  }
}
