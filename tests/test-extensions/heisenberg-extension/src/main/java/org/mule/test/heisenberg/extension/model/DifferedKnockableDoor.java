/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;

@XmlHints(allowTopLevelDefinition = true)
public class DifferedKnockableDoor {

  @Parameter
  @Optional
  private ParameterResolver<String> victim;

  @Parameter
  @Optional
  private TypedValue<String> address;

  public ParameterResolver<String> getVictim() {
    return victim;
  }

  public TypedValue<String> getAddress() {
    return address;
  }
}
