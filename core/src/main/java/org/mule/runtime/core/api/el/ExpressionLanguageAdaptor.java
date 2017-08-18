/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.el;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;

import java.util.Iterator;

/**
 * Binds Mule Core concepts {@link InternalEvent} or {@link FlowConstruct} and executes the underlying {@link ExpressionLanguage}.
 *
 * @since 4.0
 */
public interface ExpressionLanguageAdaptor {

  /**
   * Registers the given {@link BindingContext} entries as globals. Notice globals cannot be removed once registered, only
   * overwritten by the registration of a binding with the same identifier. Implementations should be thread safe to avoid race
   * conditions between registration and expression evaluation.
   *
   * @param bindingContext the context to register
   */
  void addGlobalBindings(BindingContext bindingContext);

  /**
   * Evaluates an expression according to a given {@link BindingContext}, an {@link InternalEvent} and a {@link FlowConstruct}.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param componentLocation the location of the component where the event is being processed
   * @param bindingContext the bindings to consider
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, InternalEvent event, ComponentLocation componentLocation, BindingContext bindingContext)
      throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to a given {@link BindingContext}.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, InternalEvent event, BindingContext context) throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to a given {@link BindingContext} and outputs .
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param expectedOutputType the expected output type so that automatic conversion can be performed for the resulting value
   *        type.
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, DataType expectedOutputType, InternalEvent event, BindingContext context)
      throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to a given {@link BindingContext}, an {@link InternalEvent} and a {@link FlowConstruct}.
   *
   * @param expression the expression to be executed
   * @param expectedOutputType the expected output type so that automatic conversion can be performed for the resulting value
   *        type.
   * @param event the current event being processed
   * @param componentLocation the location of the component where the event is being processed
   * @param context the bindings to consider
   * @param failOnNull indicates if should fail if the evaluation result is {@code null}
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, DataType expectedOutputType, InternalEvent event, ComponentLocation componentLocation,
                      BindingContext context,
                      boolean failOnNull)
      throws ExpressionRuntimeException;

  /**
   * Verifies whether an expression is valid or not syntactically.
   *
   * @param expression to be validated
   * @return a {@link ValidationResult} indicating whether the validation was successful or not
   */
  ValidationResult validate(String expression);

  /**
   * Splits using the specified expression and group it with the batch size. If batch size is less or equals to zero then no
   * batching is done. The expression should return a collection of elements.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param componentLocation the location of the component where the event is being processed
   * @param bindingContext the bindings to consider
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  Iterator<TypedValue<?>> split(String expression, InternalEvent event, ComponentLocation componentLocation,
                                BindingContext bindingContext)
      throws ExpressionRuntimeException;

  /**
   * Splits using the specified expression and group it with the batch size. If batch size is less or equals to zero then no
   * batching is done. The expression should return a collection of elements.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param bindingContext the bindings to consider
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  Iterator<TypedValue<?>> split(String expression, InternalEvent event, BindingContext bindingContext)
      throws ExpressionRuntimeException;
}
