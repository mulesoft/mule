/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.data.sample.extension.provider;

import org.mule.sdk.api.data.sample.SampleDataException;

public class CustomSampleDataException extends SampleDataException {

  public CustomSampleDataException(String message, String failureCode) {
    super(message, failureCode);
  }

  public CustomSampleDataException(String message, String failureCode, Throwable cause) {
    super(message, failureCode, cause);
  }
}
