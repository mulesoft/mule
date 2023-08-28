/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.exception;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleFatalException;
import org.mule.sdk.api.runtime.exception.ExceptionHandler;

public class SdkHeisenbergConnectionExceptionEnricher extends ExceptionHandler {

  public static final String ENRICHED_MESSAGE = "Enriched Connection Exception: ";

  @Override
  public Exception enrichException(Exception e) {
    if (e instanceof MuleFatalException) {
      return e;
    }
    return new ConnectionException(ENRICHED_MESSAGE + e.getMessage(), e);
  }
}
