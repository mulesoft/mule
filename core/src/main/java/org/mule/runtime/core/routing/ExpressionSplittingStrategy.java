/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.util.collection.SplittingStrategy;

import java.util.Collections;
import java.util.Iterator;

/**
 * {@link SplittingStrategy} implementation that splits based on an expression.
 * 
 * @since 4.0
 */
public class ExpressionSplittingStrategy implements SplittingStrategy<Event, Iterator<TypedValue<?>>> {

  public static final String DEFAULT_SPIT_EXPRESSION = "#[payload]";
  private final String expression;
  private final ExpressionManager expressionManager;

  /**
   * Creates a new {@link ExpressionSplittingStrategy}
   * 
   * @param expressionManager expression manager to use to evaluate the expression
   * @param expression the expression to use to split and get a collection of items
   */
  public ExpressionSplittingStrategy(ExpressionManager expressionManager, String expression) {
    this.expressionManager = expressionManager;
    this.expression = expression;
  }

  /**
   * Creates a new {@link ExpressionSplittingStrategy} and uses as default the #[payload] expression
   *
   * @param expressionManager expression manager to use to evaluate the expression
   */
  public ExpressionSplittingStrategy(ExpressionManager expressionManager) {
    this(expressionManager, DEFAULT_SPIT_EXPRESSION);
  }

  @Override
  public Iterator<TypedValue<?>> split(Event event) {
    Iterator<TypedValue<?>> result = expressionManager.split(expression, event, BindingContext.builder().build());
    return result != null ? result : Collections.<TypedValue<?>>emptyList().iterator();
  }

  /**
   * @return true if the expression was not configured or the configured one is the default one.
   */
  public boolean hasDefaultExpression() {
    return DEFAULT_SPIT_EXPRESSION.equals(expression);
  }

  /**
   * @return the expression configured in the strategy
   */
  public String getExpression() {
    return expression;
  }
}
