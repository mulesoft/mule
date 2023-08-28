/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.connection;

import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.message.ErrorType;

import java.util.Optional;

/**
 * Adapts a sdk-api {@link org.mule.sdk.api.connectivity.ConnectionValidationResult} into a mule-api
 * {@link ConnectionValidationResult}.
 *
 * @since 4.5.0
 */
public class SdkConnectionValidationResultAdapter extends ConnectionValidationResult {

  private final org.mule.sdk.api.connectivity.ConnectionValidationResult delegate;

  /**
   * Creates a new instance
   *
   * @param delegate the adapted instance
   */
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
