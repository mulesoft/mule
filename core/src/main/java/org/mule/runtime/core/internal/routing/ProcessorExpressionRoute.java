/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Objects.requireNonNull;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.processor.Processor;

/**
 * A holder for a pair of MessageProcessor and an expression.
 */
public class ProcessorExpressionRoute extends ProcessorRoute {

  private final String expression;

  public ProcessorExpressionRoute(String expression, Processor processor) {
    super(processor);
    requireNonNull(expression, "expression can't be null");
    this.expression = expression;
  }

  public String getExpression() {
    return expression;
  }

  @Override
  public boolean accepts(ExpressionManagerSession session) {
    return session.evaluateBoolean(expression, false, true);
  }
}
