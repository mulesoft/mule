/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.expression;

import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.config.i18n.CoreMessages;

/**
 * A simple configuration object for holding the common Expression evaluator configuration. The
 * {@link #getFullExpression(ExpressionManager)} will return the evaluator and expression information in a format that can be
 * passed into the {@link DefaultExpressionManager}
 */
public class ExpressionConfig {

  private String unParsedExpression;
  private String expression;

  private String fullExpression;

  private String expressionPrefix = ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
  private String expressionPostfix = ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
  private volatile boolean parsed = false;

  public ExpressionConfig() {
    super();
  }

  public ExpressionConfig(String expression) {
    this(expression, ExpressionManager.DEFAULT_EXPRESSION_PREFIX, ExpressionManager.DEFAULT_EXPRESSION_POSTFIX);

  }

  public ExpressionConfig(String expression, String expressionPrefix, String expressionPostfix) {
    setExpression(expression);
    this.expressionPostfix = expressionPostfix;
    this.expressionPrefix = expressionPrefix;
  }

  public void parse(String expressionString) {
    if (parsed) {
      return;
    }

    synchronized (this) {
      if (parsed) {
        return;
      }

      doParse(expressionString);

      parsed = true;
    }
  }

  private void doParse(String expressionString) {
    if (expressionString.startsWith(expressionPrefix)) {
      expressionString = expressionString.substring(expressionPrefix.length());
      expressionString = expressionString.substring(0, expressionString.length() - expressionPostfix.length());
    }
    this.expression = expressionString;
  }

  public void validate(ExpressionManager manager) {
    if (expression == null) {
      parse(unParsedExpression);
    }
    if (expression == null) {
      throw new IllegalArgumentException(CoreMessages.objectIsNull("expression").getMessage());
    }
  }

  public String getFullExpression(ExpressionManager manager) {
    if (fullExpression == null) {
      if (expression == null) {
        parse(unParsedExpression);
      }
      validate(manager);
      fullExpression = expressionPrefix + expression + expressionPostfix;
    }
    return fullExpression;
  }

  public String getExpression() {
    if (expression == null) {
      parse(unParsedExpression);
    }
    return expression;
  }

  public void setExpression(String expression) {
    this.unParsedExpression = expression;
    this.expression = null;
    this.fullExpression = null;
    this.parsed = false;
  }
}
