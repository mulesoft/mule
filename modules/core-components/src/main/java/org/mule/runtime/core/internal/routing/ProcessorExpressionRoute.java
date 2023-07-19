/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Objects.requireNonNull;

import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.tracer.customization.api.InitialSpanInfoProvider;

/**
 * Represents a route with the {@link Processor} it leads to, along with an expression that should be true for the route to be
 * taken.
 *
 * @since 4.3.0
 */
public class ProcessorExpressionRoute extends ProcessorRoute {

  private final String expression;


  public ProcessorExpressionRoute(String expression, Processor processor,
                                  InitialSpanInfoProvider initialSpanInfoProvider) {
    super(processor, initialSpanInfoProvider);
    this.expression = requireNonNull(expression, "expression can't be null");
  }

  public String getExpression() {
    return expression;
  }

  @Override
  public boolean accepts(ExpressionManagerSession session) {
    return session.evaluateBoolean(expression, false, true);
  }
}
