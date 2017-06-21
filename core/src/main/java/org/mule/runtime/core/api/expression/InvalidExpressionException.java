/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.expression;

import org.mule.runtime.core.api.config.i18n.CoreMessages;

/**
 * Is thrown explicitly when an expression is Malformed or invalid. Malformed means the syntax is not correct, but an expression
 * can be invalid if it refers to an expression namespace or function that does not exist
 */
public class InvalidExpressionException extends ExpressionRuntimeException {

  private String expression;

  private String message;

  public InvalidExpressionException(String expression, String message) {
    super(CoreMessages.createStaticMessage(message + ". Offending expression string is: " + expression));
    this.expression = expression;
    this.message = message;
  }

  public String getExpression() {
    return expression;
  }

  public String getMessage() {
    return message;
  }

}
