/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.error;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;

import java.util.List;

/**
 * Implementation of {@link Throwable} which also implements {@link Error}. It's used to set an {link Error} as the cause of
 * another. We need an adapter for this because the {@link Error#getCause()} method returns a {@link Throwable}.
 */
public class ThrowableError extends Throwable implements Error {

  private static final long serialVersionUID = 7595432995248792091L;

  private final Error error;

  public static ThrowableError wrap(Error error) {
    if (error instanceof ThrowableError) {
      return (ThrowableError) error;
    } else {
      return new ThrowableError(error);
    }
  }

  private ThrowableError(Error error) {
    this.error = error;
  }

  @Override
  public String getDescription() {
    return error.getDescription();
  }

  @Override
  public String getDetailedDescription() {
    return error.getDetailedDescription();
  }

  @Override
  public String getFailingComponent() {
    return error.getFailingComponent();
  }

  @Override
  public ErrorType getErrorType() {
    return error.getErrorType();
  }

  @Override
  public Message getErrorMessage() {
    return error.getErrorMessage();
  }

  @Override
  public List<Error> getChildErrors() {
    return error.getChildErrors();
  }

  @Override
  public Throwable getCause() {
    return error.getCause();
  }
}
