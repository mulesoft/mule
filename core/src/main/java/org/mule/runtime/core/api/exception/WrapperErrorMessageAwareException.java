/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.ErrorMessageAwareException;

/**
 * {@link ErrorMessageAwareException} implementation that wraps an actual exception and can be treated as a {@link MuleException}.
 *
 * @since 4.0
 */
public class WrapperErrorMessageAwareException extends MuleException implements ErrorMessageAwareException {

  private Message errorMessage;
  private Throwable exception;

  public WrapperErrorMessageAwareException(Message errorMessage, Throwable exception) {
    super(exception);
    this.errorMessage = errorMessage;
    this.exception = exception;
  }

  public Message getErrorMessage() {
    return errorMessage;
  }

  public Throwable getRootCause() {
    return exception;
  }
}
