/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
  private ParameterResolver<ParameterResolver<org.mule.sdk.api.runtime.parameter.ParameterResolver<ParameterResolver<TypedValue<InputStream>>>>> nestedParameter;

  @Parameter
  @Optional
  private ParameterResolver<org.mule.sdk.api.runtime.parameter.Literal<String>> resolverOfLiteral;

  @Parameter
  @Optional
  private ParameterResolver<TypedValue<String>> lazyString;

  public ParameterResolver<TypedValue<KnockeableDoor>> getDoorResolver() {
    return doorResolver;
  }

  public ParameterResolver<TypedValue<InputStream>> getLazyParameter() {
    return lazyParameter;
  }

  public ParameterResolver<ParameterResolver<org.mule.sdk.api.runtime.parameter.ParameterResolver<ParameterResolver<TypedValue<InputStream>>>>> getNestedParameter() {
    return nestedParameter;
  }

  public ParameterResolver<org.mule.sdk.api.runtime.parameter.Literal<String>> getResolverOfLiteral() {
    return resolverOfLiteral;
  }

  public ParameterResolver<TypedValue<String>> getLazyString() {
    return lazyString;
  }
}
