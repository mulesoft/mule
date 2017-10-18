/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.el;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.internal.message.InternalMessage;

import java.util.Iterator;

/**
 * Provides universal access for evaluating expressions embedded in Mule configurations, such as XML, Java, scripting and
 * annotations.
 */
public interface ExpressionManager extends MuleExpressionLanguage {

  String DEFAULT_EXPRESSION_PREFIX = "#[";
  String DEFAULT_EXPRESSION_POSTFIX = "]";

  /**
   * Execute the expression returning the result. The expression will be executed by the expression language implementation
   * without making any event or message context available.
   *
   * @param expression the expression to be executed
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression) throws ExpressionRuntimeException;

  /**
   * Execute the expression returning the result. The expression will be executed with MuleEvent context, meaning the expression
   * language implementation should provided access to the message.
   * <p>
   * This version of {@code evaluate} performs expression evaulation on an immutable event. Any {@link CoreEvent} or
   * {@link InternalMessage} mutation performed within the expression will impact within the context of expression evaluation but
   * will not mutated the {@code event} parameter.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, CoreEvent event) throws ExpressionRuntimeException;

  /**
   * Execute the expression returning the result. The expression will be executed with MuleEvent context, meaning the expression
   * language implementation should provided access to the message.
   * <p>
   * This version of {@code evaluate} performs expression evaulation on an immutable event. Any {@link CoreEvent} or
   * {@link InternalMessage} mutation performed within the expression will impact within the context of expression evaluation but
   * will not mutated the {@code event} parameter.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param componentLocation the location of the component where the event is being processed
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, CoreEvent event, ComponentLocation componentLocation)
      throws ExpressionRuntimeException;

  /**
   * Execute the expression returning the result. The expression will be executed with MuleEvent context, meaning the expression
   * language implementation should provided access to the message.
   * <p>
   * This version of {@code evaluate} performs expression evaulation on an immutable event. Any {@link CoreEvent} or
   * {@link InternalMessage} mutation performed within the expression will impact within the context of expression evaluation but
   * will not mutated the {@code event} parameter.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param context the bindings to be considered
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, CoreEvent event, BindingContext context) throws ExpressionRuntimeException;

  /**
   * Execute the expression returning the result. The expression will be executed with MuleEvent context, meaning the expression
   * language implementation should provided access to the message. A Map of variables can be provided that will be able to the
   * expression when executed. Variable provided in the map will only be available if there are no conflict with context variables
   * provided by the expression language implementation.
   * <p>
   * This version of {@code evaluate} performs expression evaulation on an immutable event. Any {@link CoreEvent} or
   * {@link InternalMessage} mutation performed within the expression will impact within the context of expression evaluation but
   * will not mutated the {@code event} parameter.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param componentLocation the location of the component where the event is being processed
   * @param context the bindings to be considered
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, CoreEvent event, ComponentLocation componentLocation, BindingContext context)
      throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to the global {@link BindingContext} and the {@link DataType} of the expected result.
   *
   * @param expression the EL expression
   * @param expectedOutputType the expected output type so that automatic conversion can be performed for the resulting value
   *        type.
   * @return the result of the expression plus its type
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression or during transformation
   */
  TypedValue evaluate(String expression, DataType expectedOutputType) throws ExpressionRuntimeException;


  /**
   * Evaluates an expression according to a given {@link BindingContext}, the global one, the {@link DataType} of the expected
   * result and an {@link CoreEvent}.
   *
   * @param expression the EL expression
   * @param expectedOutputType the expected output type so that automatic conversion can be performed for the resulting value
   *        type.
   * @param context an expression binding context to consider
   * @param event the current event to consider
   * @return the result of the expression plus its type
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression or during transformation
   */
  TypedValue evaluate(String expression, DataType expectedOutputType, BindingContext context, CoreEvent event)
      throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to a given {@link BindingContext}, the global one, the {@link DataType} of the expected
   * result and an {@link CoreEvent}.
   *
   * @param expression the EL expression
   * @param expectedOutputType the expected output type so that automatic conversion can be performed for the resulting value
   *        type.
   * @param context an expression binding context to consider
   * @param event the current event to consider
   * @param componentLocation the location of the component where the event is being processed
   * @param failOnNull indicates if should fail if the evaluation result is {@code null}
   * @return the result of the expression plus its type
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression or during transformation
   */
  TypedValue evaluate(String expression, DataType expectedOutputType, BindingContext context, CoreEvent event,
                      ComponentLocation componentLocation, boolean failOnNull)
      throws ExpressionRuntimeException;

  /**
   * Evaluates an expression considering a {@code boolean} as output. If the result cannot be clearly transformed or is
   * {@code null}, {@code false} will be returned.
   *
   * @param expression a single expression to be evaluated and transformed
   * @param event the {@link CoreEvent} to consider
   * @param componentLocation the location of the component where the event is being processed
   * @return {@link true} if the expression evaluated to that or "true", false otherwise
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  boolean evaluateBoolean(String expression, CoreEvent event, ComponentLocation componentLocation)
      throws ExpressionRuntimeException;

  /**
   * Evaluates an expression considering a {@code boolean} as output.
   *
   * @param expression a single expression to be evaluated and transformed
   * @param event the {@link CoreEvent} to consider
   * @param componentLocation the location of the component where the event is being processed
   * @param nullReturnsTrue whether or not a {@link null} outcome should be considered a {@link true}
   * @param nonBooleanReturnsTrue whether or not a non boolean outcome should be considered a {@link true}
   * @return {@link true} if the expression evaluated to that, "true" or the above flags where considered, {@link false} otherwise
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  boolean evaluateBoolean(String expression, CoreEvent event, ComponentLocation componentLocation, boolean nullReturnsTrue,
                          boolean nonBooleanReturnsTrue)
      throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to a given {@link BindingContext}, an {@link CoreEvent} and a flowName.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param componentLocation the location of the component where the event is being processed
   * @param bindingContext the bindings to consider
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  Iterator<TypedValue<?>> split(String expression, CoreEvent event, ComponentLocation componentLocation,
                                BindingContext bindingContext)
      throws ExpressionRuntimeException;


  /**
   * Evaluates an expression according to a given {@link BindingContext}, an {@link CoreEvent}.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param bindingContext the bindings to consider
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  Iterator<TypedValue<?>> split(String expression, CoreEvent event, BindingContext bindingContext)
      throws ExpressionRuntimeException;

  /**
   * Parses a logging expression template by iterating through each expression and evaluating it. If a user needs to evaluate a
   * single expression they can use {@link #evaluate(String, CoreEvent, ComponentLocation, BindingContext)}.
   *
   * @param template the string template featuring inner expressions to parse
   * @param event the current event being processed
   * @param componentLocation the location of the component where the event is being processed
   * @param bindingContext the bindings to consider
   * @return the result of the evaluation
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  String parseLogTemplate(String template, CoreEvent event, ComponentLocation componentLocation, BindingContext bindingContext)
      throws ExpressionRuntimeException;

}
