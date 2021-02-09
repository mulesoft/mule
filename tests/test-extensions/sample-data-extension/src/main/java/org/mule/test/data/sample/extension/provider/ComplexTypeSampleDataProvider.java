/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
