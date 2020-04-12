/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.el;

import org.mule.runtime.api.el.ExpressionLanguageSession;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;

/**
 * Evaluates an expression considering a set of given bindings passed on construction time.
 *
 * @since 4.2
 */
public interface ExpressionManagerSession extends ExpressionLanguageSession {

  /**
   * Evaluates an expression considering a {@code boolean} as output.
   *
   * @param expression a single expression to be evaluated and transformed
   * @param nullReturnsTrue whether or not a {@code null} outcome should be considered a {@code true}
   * @param nonBooleanReturnsTrue whether or not a non boolean outcome should be considered a {@code true}
   * @return {@link true} if the expression evaluated to that, "true" or the above flags where considered, {@code false} otherwise
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  boolean evaluateBoolean(String expression, boolean nullReturnsTrue, boolean nonBooleanReturnsTrue)
      throws ExpressionRuntimeException;

}
