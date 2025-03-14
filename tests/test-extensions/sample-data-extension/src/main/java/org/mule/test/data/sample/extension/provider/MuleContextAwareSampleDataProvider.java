/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension.provider;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.core.api.MuleContext;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.runtime.operation.Result;

import jakarta.inject.Inject;

public class MuleContextAwareSampleDataProvider extends ParameterizedTestSampleDataProvider {

  @Inject
  private MuleContext muleContext;

  @Override
  public Result<String, String> getSample() throws SampleDataException {
    checkArgument(muleContext != null, "muleContext is null");
    return super.getSample();
  }
}
