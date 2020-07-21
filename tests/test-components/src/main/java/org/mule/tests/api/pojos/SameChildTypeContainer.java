/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.api.pojos;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@TypeDsl(allowTopLevelDefinition = true)
public class SameChildTypeContainer {

  @Parameter
  @Optional
  private ParameterCollectionParser elementTypeA;

  @Parameter
  @Optional
  private ParameterCollectionParser anotherElementTypeA;

  public ParameterCollectionParser getElementTypeA() {
    return elementTypeA;
  }

  public void setElementTypeA(ParameterCollectionParser elementTypeA) {
    this.elementTypeA = elementTypeA;
  }

  public ParameterCollectionParser getAnotherElementTypeA() {
    return anotherElementTypeA;
  }

  public void setAnotherElementTypeA(ParameterCollectionParser anotherElementTypeA) {
    this.anotherElementTypeA = anotherElementTypeA;
  }
}
