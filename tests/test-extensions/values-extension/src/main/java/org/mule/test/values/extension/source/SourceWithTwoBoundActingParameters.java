/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
