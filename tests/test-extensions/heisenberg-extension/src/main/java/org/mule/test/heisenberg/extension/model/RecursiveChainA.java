/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.dsl.xml.TypeDsl;

import java.util.List;
import java.util.Objects;

@TypeDsl(allowTopLevelDefinition = true)
public class RecursiveChainA {

  @Parameter
  String fieldA;

  @Parameter
  RecursiveChainB chainB;

  @Parameter
  List<RecursiveChainB> bChains;

  public String getFieldA() {
    return fieldA;
  }

  public RecursiveChainB getChainB() {
    return chainB;
  }

  public List<RecursiveChainB> getbChains() {
    return bChains;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    RecursiveChainA that = (RecursiveChainA) o;
    return Objects.equals(fieldA, that.fieldA) &&
        Objects.equals(chainB, that.chainB) &&
        Objects.equals(bChains, that.bChains);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fieldA, chainB, bChains);
  }
}
