/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.source;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.binding.Binding;
import org.mule.test.values.extension.resolver.WithTwoActingParametersValueProvider;

@MediaType(TEXT_PLAIN)
public class SourceWithTwoBoundActingParameters extends AbstractSource {

  @Parameter
  @org.mule.sdk.api.annotation.values.OfValues(
      value = WithTwoActingParametersValueProvider.class,
      bindings = {@Binding(actingParameter = "requiredValue", extractionExpression = "oneParameter"),
          @Binding(actingParameter = "scalarActingParameter", extractionExpression = "someParameter")})
  private String parameterWithValue;

  @Parameter
  private String oneParameter;

  @Parameter
  private String someParameter;


}
