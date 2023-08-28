/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.data.sample.extension.provider;

import static org.mule.test.data.sample.extension.SampleDataExtension.adaptLegacy;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.test.data.sample.extension.ComplexActingParameter;
import org.mule.test.data.sample.extension.SampleDataOperations;

public class ComplexActingParameterSampleDataProvider extends TestSampleDataProvider {

  @Parameter
  private ComplexActingParameter complex;

  @Override
  public Result<String, String> getSample() throws SampleDataException {
    return adaptLegacy(new SampleDataOperations().complexActingParameter(complex));
  }
}
