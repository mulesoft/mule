/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleException;

public class WrapperErrorMessageAwareException extends MuleException implements ErrorMessageAwareException {

  private Message errorMessage;
  private Exception exception;

  public WrapperErrorMessageAwareException(Message errorMessage, Exception exception) {
    super(exception);
    this.errorMessage = errorMessage;
    this.exception = exception;
  }

  public Exception getException() {
    return exception;
  }

  public Message getErrorMessage() {
    return errorMessage;
  }
}
