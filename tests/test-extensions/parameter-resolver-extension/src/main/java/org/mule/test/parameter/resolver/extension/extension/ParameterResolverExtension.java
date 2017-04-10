/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.parameter.resolver.extension.extension;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;

@Operations(ParameterResolverParameterOperations.class)
@Extension(name = "ParameterResolver")
@Sources(SomeSource.class)
public class ParameterResolverExtension {

  public ParameterResolver<String> getStringResolver() {
    return stringResolver;
  }

  public void setStringResolver(ParameterResolver<String> stringResolver) {
    this.stringResolver = stringResolver;
  }

  public ParameterResolver<KnockeableDoor> getDoorResolver() {
    return doorResolver;
  }

  public void setDoorResolver(ParameterResolver<KnockeableDoor> doorResolver) {
    this.doorResolver = doorResolver;
  }

  @Parameter
  @Optional
  ParameterResolver<String> stringResolver;

  @Parameter
  @Optional
  @XmlHints(allowTopLevelDefinition = true)
  ParameterResolver<KnockeableDoor> doorResolver;

}
