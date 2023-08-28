/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
