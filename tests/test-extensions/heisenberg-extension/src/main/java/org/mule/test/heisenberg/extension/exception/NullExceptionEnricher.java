/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.exception;

import org.mule.sdk.api.runtime.exception.ExceptionHandler;

public class NullExceptionEnricher extends ExceptionHandler {

  @Override
  public Exception enrichException(Exception e) {
    return e;
  }
}
