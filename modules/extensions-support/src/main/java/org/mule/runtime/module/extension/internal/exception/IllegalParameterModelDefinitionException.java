/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.exception;


import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;

/**
 * A specialization of {@link IllegalModelDefinitionException} which marks that a {@link ParameterModel} is invalid
 *
 * @since 4.0
 */
public class IllegalParameterModelDefinitionException extends IllegalModelDefinitionException {

  /**
   * Creates a new instance
   *
   * @param message the detail message
   */
  public IllegalParameterModelDefinitionException(String message) {
    super(message);
  }

  /**
   * Creates a new instance
   *
   * @param message the detail message
   * @param cause the cause
   */
  public IllegalParameterModelDefinitionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new instance
   *
   * @param cause the cause
   */
  public IllegalParameterModelDefinitionException(Throwable cause) {
    super(cause);
  }
}
