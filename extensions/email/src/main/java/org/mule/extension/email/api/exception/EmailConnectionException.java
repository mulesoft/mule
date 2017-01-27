/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.exception;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * Is the base {@link ConnectionException} for the Email Connector,
 *
 * @since 4.0
 */
public class EmailConnectionException extends ConnectionException {

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   */
  public EmailConnectionException(String message) {
    super(message);
  }

  /**
   * Creates a new instance with the specified detail {@code message} and {@code error}
   *
   * @param message the detail message
   * @param error   the correspondent {@link EmailError} with the created exception
   */
  public EmailConnectionException(String message, EmailError error) {
    super(message, new ModuleException(null, error));
  }

  /**
   * Creates a new instance with the specified detail {@code message} and {@code cause}
   *
   * @param message the detail message
   * @param cause   the exception's cause
   */
  public EmailConnectionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new instance with the specified detail {@code message}, {@code cause} and {@code error}
   *
   * @param message the detail message
   * @param cause   the exception's cause
   * @param error   the correspondent {@link EmailError} with the created exception
   */
  public EmailConnectionException(String message, Throwable cause, EmailError error) {
    super(message, new ModuleException(cause, error));
  }
}
