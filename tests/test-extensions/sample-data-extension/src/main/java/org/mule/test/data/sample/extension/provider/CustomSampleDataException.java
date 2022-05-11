/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
