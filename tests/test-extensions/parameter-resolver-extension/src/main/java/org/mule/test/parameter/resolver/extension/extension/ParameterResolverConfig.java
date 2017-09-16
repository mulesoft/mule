/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.parameter.resolver.extension.extension;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;

import java.io.InputStream;

@Configuration(name = "config")
@Operations(ParameterResolverParameterOperations.class)
@Sources(SomeSource.class)
public class ParameterResolverConfig extends ParameterResolverExtension {

  @Parameter
  @Optional
  private ParameterResolver<String> stringResolver;

  @Parameter
  @Optional
  private ParameterResolver<KnockeableDoor> doorResolver;

  @Parameter
  @Optional
  private Literal<KnockeableDoor> literalDoor;

  @Parameter
  @Optional
  private ParameterResolver<TypedValue<InputStream>> lazyParameter;

  @Parameter
  @Optional
  private ParameterResolver<ParameterResolver<ParameterResolver<ParameterResolver<TypedValue<InputStream>>>>> nestedParameter;

  @Parameter
  @Optional
  private ParameterResolver<Literal<String>> resolverOfLiteral;

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

  public Literal<KnockeableDoor> getLiteralDoor() {
    return literalDoor;
  }

  public void setLiteralDoor(Literal<KnockeableDoor> literalDoor) {
    this.literalDoor = literalDoor;
  }
}
