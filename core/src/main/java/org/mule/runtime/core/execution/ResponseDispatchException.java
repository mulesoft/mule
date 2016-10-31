/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Exception thrown when there's a failure writing the response using the transport infrastructure.
 */
public class ResponseDispatchException extends MuleException {

  public ResponseDispatchException(I18nMessage message) {
    super(message);
  }

  public ResponseDispatchException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  public ResponseDispatchException(Throwable cause) {
    super(cause);
  }

}
