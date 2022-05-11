/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.el.ExpressionLanguageSession;
import org.mule.runtime.api.el.ExpressionCompilationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.CorrelationIdGenerator;
import org.mule.runtime.core.api.el.ExpressionManager;

import static java.lang.String.format;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

public class ExpressionCorrelationIdGenerator implements CorrelationIdGenerator {

  private String expression;

  private MuleContext context;

  private ExpressionManager manager;
  private CompiledExpression compiledExpression;

  public ExpressionCorrelationIdGenerator(MuleContext context, String expression) {
    this.expression = expression;
    this.context = context;
  }

  @Override
  public String generateCorrelationId() {
    try (ExpressionLanguageSession session = manager.openSession(NULL_BINDING_CONTEXT)) {
      return session.evaluate(compiledExpression).getValue().toString();
    }
  }

  private void validateExpression() {
    if (!manager.isExpression(expression) || !manager.isValid(expression)) {
      throw new ExpressionCompilationException(createStaticMessage(format("Invalid Correlation ID Generation expression: %s",
                                                                          expression)));
    }
  }

  // TODO (MULE-19231): remove this from here (won't be more necessary)
  public void initializeGenerator() {
    manager = context.getExpressionManager();
    compiledExpression = manager.compile(expression, NULL_BINDING_CONTEXT);
    validateExpression();
  }

}
