/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.expression;

import org.mule.api.transport.MessageAdapter;

/**
 * Provides universal access for evaluating expressions embedded in Mule configurations, such  as Xml, Java,
 * scripting and annotations.
 * <p/>
 * Users can register or unregister {@link ExpressionEvaluator} through this interface.
 */
public interface ExpressionManager
{
    String DEFAULT_EXPRESSION_PREFIX = "#[";
    String DEFAULT_EXPRESSION_POSTFIX = "]";

    public void registerEvaluator(ExpressionEvaluator evaluator);

    /**
     * Checks whether an evaluator is registered with the manager
     *
     * @param name the name of the expression evaluator
     * @return true if the evaluator is registered with the manager, false otherwise
     */
    public boolean isEvaluatorRegistered(String name);

    /**
     * Removes the evaluator with the given name
     *
     * @param name the name of the evaluator to remove
     * @return the evaluator that was removed. This will be null if the evaluator was not registered
     */
    public ExpressionEvaluator unregisterEvaluator(String name);

    /**
     * Evaluates the given expression.  The expression should be a single expression definition with or without
     * enclosing braces. i.e. "mule:serviceName" and "#[mule:serviceName]" are both valid. For situations where
     * one or more expressions need to be parsed within a single text, the {@link #parse(String,org.mule.api.transport.MessageAdapter,boolean)}
     * method should be used since it will iterate through all expressions in a string.
     *
     * @param expression a single expression i.e. xpath://foo
     * @param message The current message being processed
     * @return the result of the evaluation
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and
     *                                    'failIfNull is set to true.
     */
    public Object evaluate(String expression, MessageAdapter message) throws ExpressionRuntimeException;

    /**
     * Evaluates the given expression.  The expression should be a single expression definition with or without
     * enclosing braces. i.e. "mule:serviceName" and "#[mule:serviceName]" are both valid. For situations where
     * one or more expressions need to be parsed within a single text, the {@link #parse(String,org.mule.api.transport.MessageAdapter,boolean)}
     * method should be used since it will iterate through all expressions in a string.
     *
     * @param expression a single expression i.e. xpath://foo
     * @param message The current message being processed
     * @param failIfNull determines if an exception should be thrown if expression could not be evaluated or returns
     *                   null. @return the result of the evaluation
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and
     *                                    'failIfNull is set to true.
     */
    public Object evaluate(String expression, MessageAdapter message, boolean failIfNull) throws ExpressionRuntimeException;

    /**
     * Evaluates the given expression.  The expression should be a single expression definition with or without
     * enclosing braces. i.e. "mule:serviceName" and "#[mule:serviceName]" are both valid. For situations where
     * one or more expressions need to be parsed within a single text, the {@link #parse(String,org.mule.api.transport.MessageAdapter,boolean)}
     * method should be used since it will iterate through all expressions in a string.
     *
     * @param expression one or more expressions ebedded in a literal string i.e. "Value is #[xpath://foo] other value is #[header:foo]."
     * @param message The current message bing processed
     * @param evaluator  the evaluator to use when executing the expression
     * @param failIfNull determines if an exception should be thrown if expression could not be evaluated or returns
     *                   null. @return the result of the evaluation
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and
     *                                    'failIfNull is set to true.
     */
    public Object evaluate(String expression, String evaluator, MessageAdapter message, boolean failIfNull) throws ExpressionRuntimeException;


    /**
     * Evaluates expressions in a given string. This method will iterate through each expression and evaluate it. If
     * a user needs to evaluate a single expression they can use {@link #evaluate(String,org.mule.api.transport.MessageAdapter,boolean)}.
     *
     * @param expression one or more expressions ebedded in a literal string i.e. "Value is #[xpath://foo] other value is #[header:foo]."
     * @param message The current message being processed
     * @return the result of the evaluation
     * @throws org.mule.api.expression.ExpressionRuntimeException
     *          if the expression is invalid, or a null is found for the expression and
     *          'failIfNull is set to true.
     */
    public String parse(String expression, MessageAdapter message) throws ExpressionRuntimeException;

    /**
     * Evaluates expressions in a given string. This method will iterate through each expression and evaluate it. If
     * a user needs to evaluate a single expression they can use {@link #evaluate(String,org.mule.api.transport.MessageAdapter,boolean)}.
     *
     * @param expression one or more expressions ebedded in a literal string i.e. "Value is #[xpath://foo] other value is #[header:foo]."
     * @param message The current message being processed
     * @param failIfNull determines if an exception should be thrown if expression could not be evaluated or returns
     *                   null. @return the result of the evaluation
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and
     *                                    'failIfNull is set to true.
     */
    public String parse(final String expression, final MessageAdapter message, final boolean failIfNull) throws ExpressionRuntimeException;

    /**
     * Clears all registered evaluators from the manager.
     */
    public void clearEvaluators();

    /**
     * Determines if the expression is valid or not.  This method will validate a single expression or
     * expressions embedded in a string.  the expression must be well formed i.e. #[bean:user]
     *
     * @param expression the expression to validate
     * @return true if the expression evaluator is recognised
     */
    public boolean isValidExpression(String expression);
}
