/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.expression;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Thrown by the {@link org.mule.runtime.core.api.el.ExpressionManager} when a failure occurs evaluating an expression.
 */
public class ExpressionRuntimeException extends MuleRuntimeException {

  /**
   * @param message the exception message
   */
  public ExpressionRuntimeException(I18nMessage message) {
    super(message);
  }

  /**
   * @param message the exception message
   * @param cause the exception that triggered this exception
   */
  public ExpressionRuntimeException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }
}
