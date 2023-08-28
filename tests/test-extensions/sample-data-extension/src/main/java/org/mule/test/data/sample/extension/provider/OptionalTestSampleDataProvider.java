/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.data.sample.extension.provider;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.test.data.sample.extension.SampleDataOperations;

import static org.mule.test.data.sample.extension.SampleDataExtension.adaptLegacy;

public class OptionalTestSampleDataProvider extends TestSampleDataProvider {

  @Parameter
  @Optional
  protected String payload;

  @Parameter
  @Optional(defaultValue = "DEFAULT_VALUE")
  protected String attributes;

  @Override
  public Result<String, String> getSample() throws SampleDataException {
    return adaptLegacy(new SampleDataOperations().connectionLess(payload, attributes));
  }
}
