/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension.provider;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.runtime.operation.Result;

public class FailingTestSampleDataProvider extends TestSampleDataProvider {

  public static final String CUSTOM_SAMPLE_DATA_EXCEPTION_ERROR_MSG = "Custom sample data error";
  public static final String SAMPLE_DATA_EXCEPTION_ERROR_MSG = "Sample data error";
  public static final String SAMPLE_DATA_EXCEPTION_FAILURE = "SAMPLE_DATA_FAILURE";
  public static final String EXCEPTION_CAUSE_ERROR_MSG = "Internal issue";

  @Parameter
  protected String payload;

  @Parameter
  protected String attributes;

  @Parameter
  @Optional
  boolean useCustomSampleDataException;

  @Parameter
  boolean withExceptionCause;

  @Override
  public Result<String, String> getSample() throws SampleDataException {
    throw (useCustomSampleDataException ? createCustomSampleDataException() : createSampleDataException());
  }

  private SampleDataException createSampleDataException() {
    return withExceptionCause ? new SampleDataException(SAMPLE_DATA_EXCEPTION_ERROR_MSG, SAMPLE_DATA_EXCEPTION_FAILURE,
                                                        new IllegalStateException(EXCEPTION_CAUSE_ERROR_MSG))
        : new SampleDataException(SAMPLE_DATA_EXCEPTION_ERROR_MSG, SAMPLE_DATA_EXCEPTION_FAILURE);
  }

  private CustomSampleDataException createCustomSampleDataException() {
    return withExceptionCause
        ? new CustomSampleDataException(CUSTOM_SAMPLE_DATA_EXCEPTION_ERROR_MSG, SAMPLE_DATA_EXCEPTION_FAILURE,
                                        new IllegalStateException(EXCEPTION_CAUSE_ERROR_MSG))
        : new CustomSampleDataException(CUSTOM_SAMPLE_DATA_EXCEPTION_ERROR_MSG, SAMPLE_DATA_EXCEPTION_FAILURE);
  }

}
