/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.config;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.binding.Binding;
import org.mule.test.values.extension.resolver.WithRequiredParameterSdkValueProvider;

@Configuration(name = "with-bound-value-parameter")
public class ConfigWithBoundValueParameter {

  @Parameter
  @org.mule.sdk.api.annotation.values.OfValues(
      value = WithRequiredParameterSdkValueProvider.class,
      bindings = {@Binding(actingParameter = "requiredValue", extractionExpression = "actingParameter")})
  private String parameterWithValues;

  @Parameter
  private String actingParameter;
}
