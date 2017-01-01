/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.exception;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;

/**
 * Is the generic {@link RuntimeException} type for the Email Connector, used when an error could not be specified.
 *
 * @since 4.0
 */
public class EmailException extends MuleRuntimeException {

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   */
  public EmailException(String message) {
    super(createStaticMessage(message));
  }

  /**
   * Creates a new instance with the specified detail {@code message} and {@code cause}
   *
   * @param message the detail message
   * @param cause the exception's cause
   */
  public EmailException(String message, Throwable cause) {
    super(createStaticMessage(message), cause);
  }
}
