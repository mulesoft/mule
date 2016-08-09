/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.exception;


import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;

/**
 * A specialization of {@link IllegalModelDefinitionException} which marks that a {@link ConnectionProvider} is invalid
 *
 * @since 4.0
 */
public class IllegalConnectionProviderModelDefinitionException extends IllegalModelDefinitionException {

  /**
   * Creates a new instance
   *
   * @param message the detail message
   */
  public IllegalConnectionProviderModelDefinitionException(String message) {
    super(message);
  }

  /**
   * Creates a new instance
   *
   * @param message the detail message
   * @param cause the cause
   */
  public IllegalConnectionProviderModelDefinitionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new instance
   *
   * @param cause the cause
   */
  public IllegalConnectionProviderModelDefinitionException(Throwable cause) {
    super(cause);
  }
}

