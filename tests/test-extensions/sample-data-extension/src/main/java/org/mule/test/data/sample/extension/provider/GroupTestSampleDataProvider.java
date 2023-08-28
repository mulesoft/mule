/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.data.sample.extension.provider;

import static org.mule.test.data.sample.extension.SampleDataExtension.adaptLegacy;

import org.mule.sdk.api.annotation.param.Optional;
import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.test.data.sample.extension.SampleDataOperations;

public class GroupTestSampleDataProvider extends TestSampleDataProvider {

  @Parameter
  private String groupParameter;

  @Parameter
  @Optional
  private String optionalParameter;

  @Override
  public Result<String, String> getSample() throws SampleDataException {
    return adaptLegacy(new SampleDataOperations().connectionLess(groupParameter, optionalParameter));
  }
}
