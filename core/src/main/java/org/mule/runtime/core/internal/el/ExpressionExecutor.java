/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import org.mule.runtime.core.internal.el.mvel.ExpressionLanguageContext;

/**
 * Wraps an expression language engine. Implementations should not wrap expression language engine exceptions, but rather the
 * {@link ExtendedExpressionLanguageAdaptor} implementation should handle them.
 * 
 * @since 3.3
 */
public interface ExpressionExecutor<T extends ExpressionLanguageContext> {

  /**
   * Execute an expression using using the provided context.
   * 
   * @param expression
   * @param context
   * @return
   * @throws native expression language
   */
  public Object execute(String expression, T context);

  /**
   * Validate the expression
   * 
   * @param expression
   */
  public void validate(String expression);

}
