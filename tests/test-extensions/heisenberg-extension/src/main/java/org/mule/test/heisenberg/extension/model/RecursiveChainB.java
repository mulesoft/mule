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
}
