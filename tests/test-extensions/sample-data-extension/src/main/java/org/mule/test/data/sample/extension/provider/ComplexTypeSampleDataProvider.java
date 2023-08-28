/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.data.sample.extension.provider;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.data.sample.SampleDataProvider;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.test.data.sample.extension.ComplexActingParameter;

import java.util.Map;

public class ComplexTypeSampleDataProvider implements SampleDataProvider<Map<String, String>, String> {

  @Override
  public String getId() {
    return getClass().getSimpleName();
  }

  @Parameter
  private ComplexActingParameter complex;

  @Override
  public Result<Map<String, String>, String> getSample() throws SampleDataException {
    return Result.<Map<String, String>, String>builder().build();
  }
}
