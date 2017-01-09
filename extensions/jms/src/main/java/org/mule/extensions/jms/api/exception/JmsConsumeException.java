/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.exception;

import static org.mule.extensions.jms.api.exception.JmsErrors.CONSUMING;
import org.mule.runtime.extension.api.exception.ModuleException;

/**
 * {@link ModuleException} to be thrown in the cases in which the received content to be written is invalid.
 *
 * @since 4.0
 */
public class JmsConsumeException extends JmsExtensionException {

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   */
  public JmsConsumeException(String message) {
    super(message, CONSUMING);
  }

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   * @param exception cause of this exception
   */
  public JmsConsumeException(String message, Exception exception) {
    super(exception, CONSUMING, message);
  }

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   * @param errorType JMS error
   */
  protected JmsConsumeException(String message, JmsErrors errorType) {
    super(message, errorType);
  }

  /**
   * Creates a new instance with the specified detail {@code message}
   *
   * @param message the detail message
   * @param errorType JMS error
   * @param exception cause of this exception
   */
  protected JmsConsumeException(String message, JmsErrors errorType, Exception exception) {
    super(exception, errorType, message);
  }
}
