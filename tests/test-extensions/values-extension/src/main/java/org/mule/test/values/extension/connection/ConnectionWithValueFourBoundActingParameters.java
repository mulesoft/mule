/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.connection;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.binding.Binding;
import org.mule.test.values.extension.resolver.WithFourActingParametersValueProvider;

@Alias("withValueFourBoundActingParameters")
public class ConnectionWithValueFourBoundActingParameters extends AbstractConnectionProvider {

  @Parameter
  @org.mule.sdk.api.annotation.values.OfValues(
      value = WithFourActingParametersValueProvider.class,
      bindings = {@Binding(actingParameter = "requiredValue", extractionExpression = "parameterOne"),
          @Binding(actingParameter = "anotherValue", extractionExpression = "parameterTwo"),
          @Binding(actingParameter = "someValue", extractionExpression = "parameterThree"),
          @Binding(actingParameter = "optionalValue", extractionExpression = "parameterFour")})
  String parameterWithValue;

  @Parameter
  String parameterOne;

  @Parameter
  String parameterTwo;

  @Parameter
  String parameterThree;

  @Parameter
  String parameterFour;
}
