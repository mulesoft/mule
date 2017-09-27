/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.parameter.resolver.extension.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.test.heisenberg.extension.model.DifferedKnockableDoor;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;

import java.io.InputStream;

public class ParameterResolverParameterOperations {

  public ParameterResolverExtension configOperation(@Config ParameterResolverExtension config) {
    return config;
  }

  public DifferedKnockableDoor resolverOperation(DifferedKnockableDoor differedDoor) {
    return differedDoor;
  }

  public ParameterResolver<KnockeableDoor> doorOperation(@Optional ParameterResolver<KnockeableDoor> door,
                                                         @Optional KnockeableDoor someDoor) {
    return door;
  }

  @MediaType(TEXT_PLAIN)
  public ParameterResolver<String> stringOperation(@Optional(
      defaultValue = "this is a string") ParameterResolver<String> string) {
    return string;
  }

  @MediaType(TEXT_PLAIN)
  public ParameterResolver<TypedValue<InputStream>> lazyValueOperation(@Optional ParameterResolver<TypedValue<InputStream>> lazyValue) {
    return lazyValue;
  }

  public PojoWithStackableTypes pojoWithStackableTypes(@Optional PojoWithStackableTypes stackableTypes) {
    return stackableTypes;
  }

}
