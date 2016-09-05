/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.el;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.expression.InvalidExpressionException;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.util.TemplateParser;

import java.util.Map;

/**
 * Allows for the execution of expressions within Mule using an expression language. Expression language implementations should
 * not only wrap an expression language runtime but populate the context such that expression can be used to access, and
 * optionally mutate, both Mule configuration and the current message being processed via expression variables. Runtime exceptions
 * should be caught and wrapped in a Mule {@link ExpressionRuntimeException} before being re-thrown.
 * 
 * @since 3.3.0
 */
public interface ExpressionLanguage {

  String DEFAULT_EXPRESSION_PREFIX = "#[";
  String DEFAULT_EXPRESSION_POSTFIX = "]";

  /**
   * Execute the expression returning the result. The expression will be executed by the expression language implementation
   * without making any event or message context available.
   * 
   * @param <T> the return type expected
   * @param expression the expression to be executed
   * @return the result of execution of the expression.
   */
  <T> T evaluate(String expression);

  /**
   * Execute the expression returning the result. The expression will be executed by the expression language implementation
   * without making any event or message context available. A Map of variables can be provided that will be able to the expression
   * when executed. Variable provided in the map will only be available if there are no conflicts with context variables provided
   * by the expression language implementation.
   * 
   * @param <T> the return type expected
   * @param expression the expression to be executed
   * @param vars a map of expression variables
   * @return the result of execution of the expression.
   */
  <T> T evaluate(String expression, Map<String, Object> vars);

  /**
   * Execute the expression returning the result. The expression will be executed with MuleEvent context, meaning the expression
   * language implementation should provided access to the message.
   * 
   * @param <T> the return type expected
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param flowConstruct the flow where the event is being processed
   * @return the result of execution of the expression.
   *
   * @deprecated When using of this method MEL expression will not be able to mutate the message or event.
   */
  <T> T evaluate(String expression, MuleEvent event, FlowConstruct flowConstruct);

  /**
   * Execute the expression returning the result. The expression will be executed with MuleEvent context, meaning the expression
   * language implementation should provided access to the message.
   *
   * @param <T> the return type expected
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param eventBuilder event builder instance used to mutate the current message or event.
   * @param flowConstruct the flow where the event is being processed
   * @return the result of execution of the expression.
   *
   * @deprecated When using of this method MEL expression will not be able to mutate the message or event.
   */
  <T> T evaluate(String expression, MuleEvent event, MuleEvent.Builder eventBuilder, FlowConstruct flowConstruct);


  /**
   * Execute the expression returning the result. The expression will be executed with MuleEvent context, meaning the expression
   * language implementation should provided access to the message. A Map of variables can be provided that will be able to the
   * expression when executed. Variable provided in the map will only be available if there are no conflict with context variables
   * provided by the expression language implementation.
   * 
   * @param <T> the return type expected
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param flowConstruct the flow where the event is being processed
   * @param vars a map of expression variables
   * @return the result of execution of the expression.
   *
   * @deprecated When using of this method MEL expression will not be able to mutate the message or event.
   */
  <T> T evaluate(String expression, MuleEvent event, FlowConstruct flowConstruct, Map<String, Object> vars);

  /**
   * Execute the expression returning the result. The expression will be executed with MuleEvent context, meaning the expression
   * language implementation should provided access to the message. A Map of variables can be provided that will be able to the
   * expression when executed. Variable provided in the map will only be available if there are no conflict with context variables
   * provided by the expression language implementation.
   *
   * @param <T> the return type expected
   * @param expression the expression to be executed
   * @param event the current event being processed
   * @param eventBuilder event builder instance used to mutate the current message or event.
   * @param flowConstruct the flow where the event is being processed
   * @param vars a map of expression variables
   * @return the result of execution of the expression.
   *
   * @deprecated When using of this method MEL expression will not be able to mutate the message or event.
   */
  <T> T evaluate(String expression, MuleEvent event, MuleEvent.Builder eventBuilder, FlowConstruct flowConstruct,
                 Map<String, Object> vars);

  /**
   * Validates the expression returning true is the expression is valid, false otherwise.. All implementors should should validate
   * expression syntactically. Semantic validation is optional.
   * 
   * @param expression
   * @return
   */
  boolean isValid(String expression);

  /**
   * Validates the expression returning. An {@link InvalidExpressionException} will be thrown is the All implementors should
   * should validate expression syntactically. Semantic validation is optional.
   * 
   * @param expression
   * @return
   */
  void validate(String expression) throws InvalidExpressionException;

  /**
   *
   * @param expression
   * @param event
   * @param flowConstruct
   * @param object
   *
   * @Deprecated  When using of this method MEL expression will only be able to enirch existing mutable payloads or properties.
   */
  void enrich(String expression, MuleEvent event, FlowConstruct flowConstruct,
              Object object);

  void enrich(String expression, MuleEvent event, MuleEvent.Builder eventBuilder, FlowConstruct flowConstruct,
              Object object);

  /**
   * Enriches a message
   *
   * @param expression a single expression i.e. header://foo that defines how the message should be enriched
   * @param event The event to be enriched
   * @param eventBuilder event builder instance used to mutate the current message or event.
   * @param flowConstruct the flow where the event is being processed
   * @param value The typed value used for enrichment
   *
   * @deprecated When using of this method MEL expression will not be able to mutate the message or event.
   */
  void enrich(String expression, MuleEvent event, MuleEvent.Builder eventBuilder, FlowConstruct flowConstruct, TypedValue value);

  /**
   * @deprecated When using of this method MEL expression will not be able to mutate the message or event.
   */
  TypedValue evaluateTyped(String expression, MuleEvent event, FlowConstruct flowConstruct);

  TypedValue evaluateTyped(String expression, MuleEvent event, MuleEvent.Builder eventBuilder, FlowConstruct flowConstruct);

  /**
   * Evaluates expressions in a given string. This method will iterate through each expression and evaluate it. If a user needs to
   * evaluate a single expression they can use {@link #evaluate(String, MuleEvent, FlowConstruct, Map)}.
   *
   * @param expression one or more expressions ebedded in a literal string i.e. "Value is #[xpath://foo] other value is
   *        #[header:foo]."
   * @param event The current event being processed
   * @param flowConstruct the flow where the event is being processed
   * @return the result of the evaluation
   * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and 'failIfNull is set
   * to true.
   */
  String parse(String expression, MuleEvent event, FlowConstruct flowConstruct) throws ExpressionRuntimeException;

  boolean evaluateBoolean(String expression, MuleEvent event, FlowConstruct flowConstruct) throws ExpressionRuntimeException;

  boolean evaluateBoolean(String expression, MuleEvent event, FlowConstruct flowConstruct, boolean nullReturnsTrue,
                          boolean nonBooleanReturnsTrue)
      throws ExpressionRuntimeException;

  boolean isExpression(String expression);
}
