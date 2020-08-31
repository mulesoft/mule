/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tooling.extensions.metadata.internal.sampledata;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.data.sample.SampleDataProvider;
import org.mule.sdk.api.runtime.operation.Result;

public class ActingParameterSampleDataProvider implements SampleDataProvider {

  @Parameter
  private String actingParameter;

  @Override
  public String getId() {
    return getClass().getSimpleName();
  }

  @Override
  public Result getSample() throws SampleDataException {
    return Result.<Void, String>builder().attributes(actingParameter).build();
  }
}
