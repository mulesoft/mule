/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
