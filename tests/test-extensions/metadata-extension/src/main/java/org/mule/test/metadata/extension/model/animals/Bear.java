/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.model.animals;

import static org.mule.test.metadata.extension.model.animals.AnimalClade.MAMMAL;
import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;

import java.util.Objects;

@TypeDsl(allowTopLevelDefinition = true)
public class Bear extends Animal {

  private String bearName;

  public String getBearName() {
    return bearName;
  }

  public void setBearName(String bearName) {
    this.bearName = bearName;
  }

  @Override
  public AnimalClade clade() {
    return MAMMAL;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Bear bear = (Bear) o;
    return Objects.equals(bearName, bear.bearName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bearName);
  }
}
