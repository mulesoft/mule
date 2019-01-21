/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;

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
