/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.el.ExpressionExecutionException;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.ExpressionLanguageSession;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.el.validation.ScopePhaseValidationMessages;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;

import java.util.Iterator;

/**
 * Binds Mule Core concepts {@link CoreEvent} or {@link FlowConstruct} and executes the underlying {@link ExpressionLanguage}.
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
   * Evaluates an expression according to a given {@link BindingContext}, an {@link CoreEvent} and a {@link FlowConstruct}.
   *
   * @param expression        the expression to be executed
   * @param event             the current event being processed
   * @param componentLocation the location of the component where the event is being processed
   * @param bindingContext    the bindings to consider
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, CoreEvent event, ComponentLocation componentLocation, BindingContext bindingContext)
      throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to a given {@link BindingContext}.
   *
   * @param expression the expression to be executed
   * @param event      the current event being processed
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, CoreEvent event, BindingContext context) throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to a given {@link BindingContext} and outputs .
   *
   * @param expression         the expression to be executed
   * @param event              the current event being processed
   * @param expectedOutputType the expected output type so that automatic conversion can be performed for the resulting value
   *                           type.
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, DataType expectedOutputType, CoreEvent event, BindingContext context)
      throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to a given {@link BindingContext}, an {@link CoreEvent} and a {@link FlowConstruct}.
   *
   * @param expression         the expression to be executed
   * @param expectedOutputType the expected output type so that automatic conversion can be performed for the resulting value
   *                           type.
   * @param event              the current event being processed
   * @param componentLocation  the location of the component where the event is being processed
   * @param context            the bindings to consider
   * @param failOnNull         indicates if should fail if the evaluation result is {@code null}
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, DataType expectedOutputType, CoreEvent event, ComponentLocation componentLocation,
                      BindingContext context,
                      boolean failOnNull)
      throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to a given {@link BindingContext} and the global one, doing a best effort to avoid failing
   * when the result value can not be represented in the corresponding format.
   *
   * @param expression        the EL expression
   * @param event             the current event being processed
   * @param componentLocation the location of the component where the event is being processed
   * @param bindingContext    the current dynamic binding context to consider
   * @return the result of the expression plus its type
   * @throws ExpressionExecutionException when an error occurs during evaluation
   */
  TypedValue<?> evaluateLogExpression(String expression, CoreEvent event, ComponentLocation componentLocation,
                                      BindingContext bindingContext)
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
   * @param expression        the expression to be executed
   * @param event             the current event being processed
   * @param componentLocation the location of the component where the event is being processed
   * @param bindingContext    the bindings to consider
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  Iterator<TypedValue<?>> split(String expression, CoreEvent event, ComponentLocation componentLocation,
                                BindingContext bindingContext)
      throws ExpressionRuntimeException;

  /**
   * Splits using the specified expression and group it with the batch size. If batch size is less or equals to zero then no
   * batching is done. The expression should return a collection of elements.
   *
   * @param expression     the expression to be executed
   * @param event          the current event being processed
   * @param bindingContext the bindings to consider
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  Iterator<TypedValue<?>> split(String expression, CoreEvent event, BindingContext bindingContext)
      throws ExpressionRuntimeException;

  ExpressionLanguageSessionAdaptor openSession(ComponentLocation componentLocation, CoreEvent event, BindingContext context);

  /**
   * Compiles the given {@code expression}.
   * <p>
   * keep in mind that the {@code bindingContext} is used for compilation only. When evaluated, the compiled expression <b>WILL
   * NOT</b> be evaluated against those bindings but the ones associated with current {@link ExpressionLanguageSession}
   *
   * @param expression     the expression to compile
   * @param bindingContext a {@link BindingContext} with example bindings of the values the real expression will need
   * @return a {@link CompiledExpression}
   * @since 4.3.0
   */
  CompiledExpression compile(String expression, BindingContext bindingContext);

  default ScopePhaseValidationMessages collectScopePhaseValidationMessages(String script, String nameIdentifier,
                                                                           TypeBindings bindings) {
    throw new UnsupportedOperationException("The bal");
  }
}
