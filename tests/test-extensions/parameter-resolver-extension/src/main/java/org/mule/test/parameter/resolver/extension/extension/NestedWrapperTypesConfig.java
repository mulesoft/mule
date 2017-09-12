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
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;

import java.io.InputStream;

@Configuration(name = "nested-types-config")
@Operations(ParameterResolverParameterOperations.class)
public class NestedWrapperTypesConfig extends ParameterResolverExtension {

  @Parameter
  @Optional
  private ParameterResolver<TypedValue<KnockeableDoor>> doorResolver;

  @Parameter
  @Optional
  private ParameterResolver<TypedValue<InputStream>> lazyParameter;

  @Parameter
  @Optional
  private ParameterResolver<ParameterResolver<ParameterResolver<ParameterResolver<TypedValue<InputStream>>>>> nestedParameter;

  @Parameter
  @Optional
  private ParameterResolver<Literal<String>> resolverOfLiteral;

  @Parameter
  @Optional
  private ParameterResolver<TypedValue<String>> lazyString;

  public ParameterResolver<TypedValue<KnockeableDoor>> getDoorResolver() {
    return doorResolver;
  }

  public ParameterResolver<TypedValue<InputStream>> getLazyParameter() {
    return lazyParameter;
  }

  public ParameterResolver<ParameterResolver<ParameterResolver<ParameterResolver<TypedValue<InputStream>>>>> getNestedParameter() {
    return nestedParameter;
  }

  public ParameterResolver<Literal<String>> getResolverOfLiteral() {
    return resolverOfLiteral;
  }

  public ParameterResolver<TypedValue<String>> getLazyString() {
    return lazyString;
  }
}
