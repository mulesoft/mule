/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.source.polling.watermark;

import static java.lang.String.format;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.el.ExpressionLanguage;

import java.io.NotSerializableException;
import java.io.Serializable;

public abstract class WatermarkUtils {

  /**
   * Evaluates a mel expression. If the value is not an expression or if it is not valid then returns the same value. The
   * expression is expected to
   * 
   * @param expression The expression the user wrote in the xml. Can be an expression or not
   * @param event The mule event in which we need to evaluate the expression
   * @return The evaluated value
   * @throws NotSerializableException if the evaluated result is not {@link Serializable}
   */
  public static Serializable evaluate(String expression, MuleEvent event, MuleContext muleContext)
      throws NotSerializableException {
    ExpressionLanguage expressionManager = muleContext.getExpressionLanguage();
    if (expressionManager.isExpression(expression) && expressionManager.isValid(expression)) {
      Object evaluated = expressionManager.evaluate(expression, event, null);
      if (evaluated != null && !(evaluated instanceof Serializable)) {
        throw new NotSerializableException(format("Expression %s resolves to an object that is not serializable (%s). It can't be used as watermark.",
                                                  expression, evaluated.getClass().getCanonicalName()));
      }

      return (Serializable) evaluated;
    }

    return expression;
  }

}
