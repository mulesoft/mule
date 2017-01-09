/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.el;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;

/**
 * Evaluates an expression considering a given context.
 *
 * @since 4.0
 */
public interface ExpressionLanguage {

  /**
   * Registers the given {@link BindingContext} as global.
   *
   * @param bindingContext the context to register
   */
  void registerGlobalContext(BindingContext bindingContext);

  /**
   * Evaluates an expression according to a given {@link BindingContext} and an {@link MuleEvent}.
   *
   * @param expression the EL expression
   * @param event the current event to consider
   * @param context an expression binding context to consider
   * @return the result of the expression plus its type
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, Event event, BindingContext context) throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to a given {@link BindingContext}, an {@link MuleEvent} and a {@link FlowConstruct}.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param flowConstruct the flow where the event is being processed
   * @param bindingContext the bindings to consider
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, Event event, FlowConstruct flowConstruct, BindingContext bindingContext)
      throws ExpressionRuntimeException;

  /**
   * Verifies whether an expression is valid or not syntactically.
   *
   * @param expression to be validated
   * @return a {@link ValidationResult} indicating whether the validation was successful or not
   */
  ValidationResult validate(String expression);

}
