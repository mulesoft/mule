/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension.source;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.binding.Binding;
import org.mule.sdk.api.annotation.data.sample.SampleData;
import org.mule.test.data.sample.extension.provider.ParameterizedTestSampleDataProvider;

@Alias("listener-with-two-bound-acting-parameters")
@SampleData(value = ParameterizedTestSampleDataProvider.class,
    bindings = {@Binding(actingParameter = "payload", extractionExpression = "payload"),
        @Binding(actingParameter = "attributes", extractionExpression = "attributes")})
@MediaType(TEXT_PLAIN)
public class SimpleTestSampleDataListenerWithTwoBoundActingParameters extends TestSampleDataListener {

  @Parameter
  private String payload;

  @Parameter
  @Optional
  private String attributes;
}
