/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import org.mule.sdk.api.runtime.exception.ExceptionHandler;

public final class NullExceptionHandler extends ExceptionHandler {

  @Override
  public Exception enrichException(Exception e) {
    return e;
  }
}
