/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.connection;

import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.message.ErrorType;

import java.util.Optional;

public class SdkConnectionValidationResultAdapter extends ConnectionValidationResult {

  private final org.mule.sdk.api.connectivity.ConnectionValidationResult delegate;

  SdkConnectionValidationResultAdapter(org.mule.sdk.api.connectivity.ConnectionValidationResult delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean isValid() {
    return delegate.isValid();
  }

  @Override
  public String getMessage() {
    return delegate.getMessage();
  }

  @Override
  public Optional<ErrorType> getErrorType() {
    return delegate.getErrorType();
  }

  @Override
  public Exception getException() {
    return delegate.getException();
  }
}
