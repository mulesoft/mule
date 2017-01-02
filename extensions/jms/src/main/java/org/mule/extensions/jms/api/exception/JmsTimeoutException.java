/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.exception;

import static org.mule.extensions.jms.api.exception.JmsErrors.TIMEOUT;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ModuleException} that represents an error when consuming a Message
 * but a timeout is reached before the Message arrives
 *
 * @since 4.0
 */
public final class JmsTimeoutException extends JmsConsumeException {

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   */
  public JmsTimeoutException(String message) {
    super(message, TIMEOUT);
  }
}
