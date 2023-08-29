/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Objects.requireNonNull;

import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;

/**
 * Represents a route with the {@link Processor} it leads to, along with an expression that should be true for the route to be
 * taken.
 *
 * @since 4.3.0
 */
public class ProcessorExpressionRoute extends ProcessorRoute {

  private final String expression;


  public ProcessorExpressionRoute(String expression, Processor processor,
                                  ComponentTracerFactory componentTracerFactory) {
    super(processor, componentTracerFactory);
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
