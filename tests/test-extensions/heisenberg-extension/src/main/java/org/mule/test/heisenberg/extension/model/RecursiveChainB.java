/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import java.util.List;
import java.util.Objects;

@TypeDsl(allowTopLevelDefinition = true)
public class RecursiveChainB {

  @Parameter
  String fieldB;

  @Parameter
  RecursiveChainA chainA;

  @Parameter
  List<RecursiveChainA> aChains;

  public String getFieldB() {
    return fieldB;
  }

  public RecursiveChainA getChainA() {
    return chainA;
  }

  public List<RecursiveChainA> getaChains() {
    return aChains;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    RecursiveChainB that = (RecursiveChainB) o;
    return Objects.equals(fieldB, that.fieldB) &&
        Objects.equals(chainA, that.chainA) &&
        Objects.equals(aChains, that.aChains);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fieldB, chainA, aChains);
  }
}
