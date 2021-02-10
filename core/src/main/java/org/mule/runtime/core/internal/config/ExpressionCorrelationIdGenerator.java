/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.CorrelationIdGenerator;
import org.mule.runtime.core.api.el.ExpressionManager;

public class ExpressionCorrelationIdGenerator implements CorrelationIdGenerator {

  private String expression;

  public ExpressionCorrelationIdGenerator(String expression) {
    this.expression = expression;
  }

  @Override
  public String generateCorrelationId(MuleContext context) {
    ExpressionManager expressionManager = context.getExpressionManager();
    return expressionManager.evaluate(expression).getValue().toString();
  }
}
