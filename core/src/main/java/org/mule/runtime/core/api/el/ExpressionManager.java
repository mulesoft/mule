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
import org.mule.runtime.core.api.expression.InvalidExpressionException;
import org.mule.runtime.core.api.message.InternalMessage;

public interface ExpressionManager {

  String DEFAULT_EXPRESSION_PREFIX = "#[";
  String DEFAULT_EXPRESSION_POSTFIX = "]";

  /**
   * Execute the expression returning the result. The expression will be executed by the expression language implementation
   * without making any event or message context available.
   *
   * @param expression the expression to be executed
   * @return the result of execution of the expression.
   */
  TypedValue evaluate(String expression);

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
   */
  TypedValue evaluate(String expression, Event event);

  /**
   * Execute the expression returning the result. The expression will be executed by the expression language implementation
   * without making any event or message context available. A Map of variables can be provided that will be able to the expression
   * when executed. Variable provided in the map will only be available if there are no conflicts with context variables provided
   * by the expression language implementation.
   *
   * @param expression the expression to be executed
   * @param context the bindings to be considered
   * @return the result of execution of the expression.
   */
  TypedValue evaluate(String expression, BindingContext context);

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
   */
  TypedValue evaluate(String expression, Event event, FlowConstruct flowConstruct);

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
   */
  TypedValue evaluate(String expression, Event event, BindingContext context);


  /**
   * Execute the expression returning the result. The expression will be executed with MuleEvent context, meaning the expression
   * language implementation should provided access to the message.
   *
   * This version of {@code evaluate} allows {@link Event} or {@link InternalMessage} mutation performed
   * within the expression to be maintained post-evaluation via the use of a result
   * {@link org.mule.runtime.core.api.Event.Builder} which should be created from the original event before being passed and then
   * used to construct the post-evaluation event.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param eventBuilder event builder instance used to mutate the current message or event.
   * @param flowConstruct the flow where the event is being processed
   * @return the result of execution of the expression.
   */
  TypedValue evaluate(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct);

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
   */
  TypedValue evaluate(String expression, Event event, FlowConstruct flowConstruct, BindingContext context);

  /**
   * Execute the expression returning the result. The expression will be executed with MuleEvent context, meaning the expression
   * language implementation should provided access to the message. A Map of variables can be provided that will be able to the
   * expression when executed. Variable provided in the map will only be available if there are no conflict with context variables
   * provided by the expression language implementation.
   *
   * This version of {@code evaluate} allows {@link Event} or {@link InternalMessage} mutation performed
   * within the expression to be maintained post-evaluation via the use of a result
   * {@link org.mule.runtime.core.api.Event.Builder} which should be created from the original event before being passed and then
   * used to construct the post-evaluation event.
   *
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param eventBuilder event builder instance used to mutate the current message or event.
   * @param flowConstruct the flow where the event is being processed
   * @param context the bindings to be considered
   * @return the result of execution of the expression.
   */
  TypedValue evaluate(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct,
                      BindingContext context);

  /**
   * Evaluates an expression according to the global {@link BindingContext} and the {@link DataType} of the expected result.
   *
   * @param expression the EL expression
   * @param expectedOutputType the expected output type so that the EL can do automatic conversion for the resulting value type.
   * @return the result of the expression plus its type
   */
  TypedValue evaluate(String expression, DataType expectedOutputType);

  /**
   * Evaluates an expression according to a given {@link BindingContext}, the global one and the {@link DataType} of the expected result.
   *
   * @param expression the EL expression
   * @param expectedOutputType the expected output type so that the EL can do automatic conversion for the resulting value type.
   * @param context an expression binding context to consider
   * @return the result of the expression plus its type
   */
  TypedValue evaluate(String expression, DataType expectedOutputType, BindingContext context);

  /**
   * Evaluates an expression according to a given {@link BindingContext}, the global one, the {@link DataType} of the expected result and an {@link MuleEvent}.
   *
   * @param expression the EL expression
   * @param expectedOutputType the expected output type so that the EL can do automatic conversion for the resulting value type.
   * @param context an expression binding context to consider
   * @param event the current event to consider
   * @return the result of the expression plus its type
   */
  TypedValue evaluate(String expression, DataType expectedOutputType, BindingContext context, Event event);

  /**
   * Enriches an event.
   *
   * This version of {@code enrich} allows {@link Event} or {@link InternalMessage} mutation performed
   * within the expression to be maintained post-evaluation via the use of a result
   * {@link org.mule.runtime.core.api.Event.Builder} which should be created from the original event before being passed and then
   * used to construct the post-evaluation event.
   *
   * @param expression
   * @param event
   * @param eventBuilder
   * @param flowConstruct
   * @param object
   */
  void enrich(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct, Object object);

  /**
   * Enriches an event using a typed value.
   *
   * This version of {@code enrich} allows {@link Event} or {@link InternalMessage} mutation performed
   * within the expression to be maintained post-evaluation via the use of a result
   * {@link org.mule.runtime.core.api.Event.Builder} which should be created from the original event before being passed and then
   * used to construct the post-evaluation event.
   *
   * @param expression a single expression i.e. header://foo that defines how the message should be enriched
   * @param event The event to be enriched
   * @param eventBuilder event builder instance used to mutate the current message or event.
   * @param flowConstruct the flow where the event is being processed
   * @param value The typed value used for enrichment
   */
  void enrich(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct, TypedValue value);

  boolean evaluateBoolean(String expression, Event event, FlowConstruct flowConstruct) throws ExpressionRuntimeException;

  boolean evaluateBoolean(String expression, Event event, FlowConstruct flowConstruct, boolean nullReturnsTrue,
                          boolean nonBooleanReturnsTrue)
      throws ExpressionRuntimeException;

  /**
   * Evaluates expressions in a given string. This method will iterate through each expression and evaluate it. If a user needs to
   * evaluate a single expression they can use {@link #evaluate(String, Event, FlowConstruct, BindingContext)}.
   *
   * This version of {@code evaluate} performs expression evaulation on an immutable event. Any {@link Event} or
   * {@link InternalMessage} mutation performed within the expression will impact within the context of
   * expression evaluation but will not mutated the {@code event} parameter.
   *
   * @param expression one or more expressions ebedded in a literal string i.e. "Value is #[xpath://foo] other value is
   *        #[header:foo]."
   * @param event The current event being processed
   * @param flowConstruct the flow where the event is being processed
   * @return the result of the evaluation
   * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and 'failIfNull is set
   *         to true.
   */
  String parse(String expression, Event event, FlowConstruct flowConstruct) throws ExpressionRuntimeException;

  /**
   * Evaluates expressions in a given string. This method will iterate through each expression and evaluate it. If a user needs to
   * evaluate a single expression they can use {@link #evaluate(String, Event, FlowConstruct, BindingContext)}.
   *
   * This version of {@code parse} allows {@link Event} or {@link InternalMessage} mutation performed
   * within the expression to be maintained post-evaluation via the use of a result
   * {@link org.mule.runtime.core.api.Event.Builder} which should be created from the original event before being passed and then
   * used to construct the post-evaluation event.
   *
   * @param expression one or more expressions embedded in a literal string i.e. "Value is #[xpath://foo] other value is
   *        #[header:foo]."
   * @param event The current event being processed
   * @param eventBuilder event builder instance used to mutate the current message or event.
   * @param flowConstruct the flow where the event is being processed
   * @return the result of the evaluation
   * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and 'failIfNull is set
   *         to true.
   */
  String parse(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct)
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
   * Validates the expression returning. An {@link InvalidExpressionException} will be thrown is the All implementors should
   * should validate expression syntactically. Semantic validation is optional.
   *
   * @param expression the expression to validate
   * @throws InvalidExpressionException if the expression is invalid, including information about the position and fault
   */
  ValidationResult validate(String expression);
}
