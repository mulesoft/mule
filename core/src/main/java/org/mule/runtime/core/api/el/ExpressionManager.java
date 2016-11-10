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
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.message.InternalMessage;

/**
 * Provides universal access for evaluating expressions embedded in Mule configurations, such as XML, Java,
 * scripting and annotations.
 */
public interface ExpressionManager {

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
   *
   * This version of {@code evaluate} performs expression evaulation on an immutable event. Any {@link Event} or
   * {@link InternalMessage} mutation performed within the expression will impact within the context of
   * expression evaluation but will not mutated the {@code event} parameter.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, Event event) throws ExpressionRuntimeException;

  /**
   * Execute the expression returning the result. The expression will be executed by the expression language implementation
   * without making any event or message context available. A Map of variables can be provided that will be able to the expression
   * when executed. Variable provided in the map will only be available if there are no conflicts with context variables provided
   * by the expression language implementation.
   *
   * @param expression the expression to be executed
   * @param context the bindings to be considered
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, BindingContext context) throws ExpressionRuntimeException;

  /**
   * Execute the expression returning the result. The expression will be executed with MuleEvent context, meaning the expression
   * language implementation should provided access to the message.
   *
   * This version of {@code evaluate} performs expression evaulation on an immutable event. Any {@link Event} or
   * {@link InternalMessage} mutation performed within the expression will impact within the context of
   * expression evaluation but will not mutated the {@code event} parameter.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param flowConstruct the flow where the event is being processed
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, Event event, FlowConstruct flowConstruct) throws ExpressionRuntimeException;

  /**
   * Execute the expression returning the result. The expression will be executed with MuleEvent context, meaning the expression
   * language implementation should provided access to the message.
   *
   * This version of {@code evaluate} performs expression evaulation on an immutable event. Any {@link Event} or
   * {@link InternalMessage} mutation performed within the expression will impact within the context of
   * expression evaluation but will not mutated the {@code event} parameter.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param context the bindings to be considered
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, Event event, BindingContext context) throws ExpressionRuntimeException;

  /**
   * Execute the expression returning the result. The expression will be executed with MuleEvent context, meaning the expression
   * language implementation should provided access to the message. A Map of variables can be provided that will be able to the
   * expression when executed. Variable provided in the map will only be available if there are no conflict with context variables
   * provided by the expression language implementation.
   *
   * This version of {@code evaluate} performs expression evaulation on an immutable event. Any {@link Event} or
   * {@link InternalMessage} mutation performed within the expression will impact within the context of
   * expression evaluation but will not mutated the {@code event} parameter.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param flowConstruct the flow where the event is being processed
   * @param context the bindings to be considered
   * @return the result of execution of the expression.
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  TypedValue evaluate(String expression, Event event, FlowConstruct flowConstruct, BindingContext context)
      throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to the global {@link BindingContext} and the {@link DataType} of the expected result.
   *
   * @param expression the EL expression
   * @param expectedOutputType the expected output type so that automatic conversion can be performed for the resulting value type.
   * @return the result of the expression plus its type
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression or during transformation
   */
  TypedValue evaluate(String expression, DataType expectedOutputType) throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to a given {@link BindingContext}, the global one and the {@link DataType} of the expected result.
   *
   * @param expression the EL expression
   * @param expectedOutputType the expected output type so that automatic conversion can be performed for the resulting value type.
   * @param context an expression binding context to consider
   * @return the result of the expression plus its type
   * @throws ExpressionRuntimeException or during transformation or during transformation
   */
  TypedValue evaluate(String expression, DataType expectedOutputType, BindingContext context) throws ExpressionRuntimeException;

  /**
   * Evaluates an expression according to a given {@link BindingContext}, the global one, the {@link DataType} of the expected result and an {@link MuleEvent}.
   *
   * @param expression the EL expression
   * @param expectedOutputType the expected output type so that automatic conversion can be performed for the resulting value type.
   * @param context an expression binding context to consider
   * @param event the current event to consider
   * @return the result of the expression plus its type
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression or during transformation
   */
  TypedValue evaluate(String expression, DataType expectedOutputType, BindingContext context, Event event)
      throws ExpressionRuntimeException;

  /**
   * Evaluates an expression considering a {@code boolean} as output. If the result cannot be clearly transformed or is
   * {@link null}, {@link false} will be returned.
   *
   * @param expression a single expression to be evaluated and transformed
   * @param event the {@link Event} to consider
   * @param flowConstruct the {@link FlowConstruct} to consider
   * @return {@link true} if the expression evaluated to that or "true", false otherwise
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  boolean evaluateBoolean(String expression, Event event, FlowConstruct flowConstruct) throws ExpressionRuntimeException;

  /**
   * Evaluates an expression considering a {@code boolean} as output.
   *
   * @param expression a single expression to be evaluated and transformed
   * @param event the {@link Event} to consider
   * @param flowConstruct the {@link FlowConstruct} to consider
   * @param nullReturnsTrue whether or not a {@link null} outcome should be considered a {@link true}
   * @param nonBooleanReturnsTrue whether or not a non boolean outcome should be considered a {@link true}
   * @return {@link true} if the expression evaluated to that, "true" or the above flags where considered, {@link false} otherwise
   * @throws ExpressionRuntimeException if a problem occurs evaluating the expression
   */
  boolean evaluateBoolean(String expression, Event event, FlowConstruct flowConstruct, boolean nullReturnsTrue,
                          boolean nonBooleanReturnsTrue)
      throws ExpressionRuntimeException;

  /**
   * Determines if the string is an expression.
   *
   * @param expression is this string an expression string
   * @return true if the string contains an expression
   */
  boolean isExpression(String expression);

  /**
   * Validates the expression returning true is the expression is valid, false otherwise.. All implementors should should validate
   * expression syntactically. Semantic validation is optional.
   *
   * @param expression
   * @return true if the expression is valid.
   */
  boolean isValid(String expression);

  /**
   * Validates the expression syntactically.
   *
   * @param expression the expression to validate
   * @return a {@link ValidationResult} indicating whether the validation was successful or not.
   */
  ValidationResult validate(String expression);
}
