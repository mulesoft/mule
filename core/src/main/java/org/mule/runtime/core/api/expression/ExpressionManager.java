/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.expression;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.metadata.TypedValue;

/**
 * Provides universal access for evaluating expressions embedded in Mule configurations, such as Xml, Java, scripting and
 * annotations.
 * <p/>
 */
public interface ExpressionManager {

  String DEFAULT_EXPRESSION_PREFIX = "#[";
  String DEFAULT_EXPRESSION_POSTFIX = "]";

  /**
   * Evaluates the given expression. The expression should be a single expression definition with or without enclosing braces.
   * i.e. "context:serviceName" and "#[context:serviceName]" are both valid. For situations where one or more expressions need to
   * be parsed within a single text, the {@link #parse(String,org.mule.runtime.core.api.MuleEvent,boolean)} method should be used
   * since it will iterate through all expressions in a string.
   * 
   * @param expression a single expression i.e. xpath://foo
   * @param event The current event being processed
   * @return the result of the evaluation
   * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and 'failIfNull is set
   *         to true.
   */
  Object evaluate(String expression, MuleEvent event) throws ExpressionRuntimeException;

  /**
   * Evaluates the given expression. The expression should be a single expression definition with or without enclosing braces.
   * i.e. "context:serviceName" and "#[context:serviceName]" are both valid. For situations where one or more expressions need to
   * be parsed within a single text, the {@link #parse(String,org.mule.runtime.core.api.MuleEvent,boolean)} method should be used
   * since it will iterate through all expressions in a string.
   * 
   * @param expression a single expression i.e. xpath://foo
   * @param event The current event being processed
   * @param failIfNull determines if an exception should be thrown if expression could not be evaluated or returns null. @return
   *        the result of the evaluation
   * @return the parsered expression string
   * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and 'failIfNull is set
   *         to true.
   */
  Object evaluate(String expression, MuleEvent event, boolean failIfNull) throws ExpressionRuntimeException;

  /**
   * Evaluates the given expression resolving the result of the evaluation to a boolean. The expression should be a single
   * expression definition with or without enclosing braces. i.e. "context:serviceName" and "#[context:serviceName]" are both
   * valid.
   * 
   * @param expression a single expression i.e. header:foo=bar
   * @param event The current event being processed
   */
  boolean evaluateBoolean(String expression, MuleEvent event) throws ExpressionRuntimeException;

  /**
   * Evaluates the given expression resolving the result of the evaluation to a boolean. The expression should be a single
   * expression definition with or without enclosing braces. i.e. "context:serviceName" and "#[context:serviceName]" are both
   * valid.
   * 
   * @param expression a single expression i.e. header:foo=bar
   * @param event The current message being processed
   * @param nullReturnsTrue determines if true should be returned if the result of the evaluation is null
   * @param nonBooleanReturnsTrue determines if true should returned if the result is not null but isn't recognised as a boolean
   */
  boolean evaluateBoolean(String expression, MuleEvent event, boolean nullReturnsTrue, boolean nonBooleanReturnsTrue)
      throws ExpressionRuntimeException;

  TypedValue evaluateTyped(String expression, MuleEvent event);

  /**
   * Enriches the current message using
   * 
   * @param expression a single expression i.e. header://foo that defines how the message should be enriched
   * @param event The current event being processed that will be enriched
   * @param object The object that will be used to enrich the message
   */
  void enrich(String expression, MuleEvent event, Object object);

  /**
   * Enriches the current message using a typed value
   *
   * @param expression a single expression i.e. header://foo that defines how the message should be enriched
   * @param event The current event being processed that will be enriched
   * @param object The typed value that will be used to enrich the message
   */
  void enrichTyped(String expression, MuleEvent event, TypedValue object);

  /**
   * Evaluates expressions in a given string. This method will iterate through each expression and evaluate it. If a user needs to
   * evaluate a single expression they can use {@link #evaluate(String,org.mule.runtime.core.api.MuleEvent,boolean)}.
   * 
   * @param expression one or more expressions ebedded in a literal string i.e. "Value is #[xpath://foo] other value is
   *        #[header:foo]."
   * @param event The current event being processed
   * @return the result of the evaluation
   * @throws org.mule.runtime.core.api.expression.ExpressionRuntimeException if the expression is invalid, or a null is found for
   *         the expression and 'failIfNull is set to true.
   */
  String parse(String expression, MuleEvent event) throws ExpressionRuntimeException;

  /**
   * Evaluates expressions in a given string. This method will iterate through each expression and evaluate it. If a user needs to
   * evaluate a single expression they can use {@link #evaluate(String,org.mule.runtime.core.api.MuleEvent,boolean)}.
   * 
   * @param expression one or more expressions ebedded in a literal string i.e. "Value is #[xpath://foo] other value is
   *        #[header:foo]."
   * @param event The current event being processed
   * @param failIfNull determines if an exception should be thrown if expression could not be evaluated or returns null. @return
   *        the result of the evaluation
   * @return the parsered expression string
   * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and 'failIfNull is set
   *         to true.
   */
  String parse(final String expression, final MuleEvent event, final boolean failIfNull) throws ExpressionRuntimeException;

  /**
   * Determines if the expression is valid or not. This method will validate a single expression or expressions embedded in a
   * string. The expression must either be a well formed expression evaluator i.e. #[bean:user] or must be a valid expression
   * language expression.
   * 
   * @param expression the expression to validate
   * @return true if the expression evaluator is recognised
   */
  boolean isValidExpression(String expression);

  /**
   * Determines if the expression is valid or not. This method will validate a single expression or expressions embedded in a
   * string. The expression must either be a well formed expression evaluator i.e. #[bean:user] or must be a valid expression
   * language expression.
   * 
   * @param expression the expression to validate
   * @throws InvalidExpressionException if the expression is invalid, including information about the position and fault
   * @since 3.0
   */
  void validateExpression(String expression) throws InvalidExpressionException;

  /**
   * Determines if the string is an expression. This method will validate that the string contains either the expression prefix
   * (malformed) or a full expression. This isn't full proof but catches most error cases
   * 
   * @param expression is this string an expression string
   * @return true if the string contains an expression
   * @since 3.0
   */
  boolean isExpression(String expression);

}
