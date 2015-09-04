/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.expression;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.transformer.types.TypedValue;

/**
 * Provides universal access for evaluating expressions embedded in Mule configurations, such as Xml, Java,
 * scripting and annotations.
 * <p/>
 * Users can register or unregister {@link ExpressionEvaluator} through this interface.
 */
public interface ExpressionManager
{
    String DEFAULT_EXPRESSION_PREFIX = "#[";
    String DEFAULT_EXPRESSION_POSTFIX = "]";

    void registerEvaluator(ExpressionEvaluator evaluator);

    /**
     * Checks whether an evaluator is registered with the manager
     * 
     * @param name the name of the expression evaluator
     * @return true if the evaluator is registered with the manager, false otherwise
     */
    boolean isEvaluatorRegistered(String name);

    /**
     * Removes the evaluator with the given name
     * 
     * @param name the name of the evaluator to remove
     * @return the evaluator that was removed. This will be null if the evaluator was not registered
     */
    ExpressionEvaluator unregisterEvaluator(String name);

    void registerEnricher(ExpressionEnricher enricher);

    /**
     * Checks whether an enricher is registered with the manager
     * 
     * @param name the name of the expression enricher
     * @return true if the enricher is registered with the manager, false otherwise
     */
    @Deprecated
    boolean isEnricherRegistered(String name);

    /**
     * Removes the enricher with the given name
     * 
     * @param name the name of the enricher to remove
     * @return the enricher that was removed. This will be null if the enricher was not registered
     */
    @Deprecated
    ExpressionEnricher unregisterEnricher(String name);

    /**
     * Evaluates the given expression. The expression should be a single expression definition with or without
     * enclosing braces. i.e. "context:serviceName" and "#[context:serviceName]" are both valid. For
     * situations where one or more expressions need to be parsed within a single text, the
     * {@link #parse(String,org.mule.api.MuleMessage,boolean)} method should be used since it will iterate
     * through all expressions in a string.
     * 
     * @param expression a single expression i.e. xpath://foo
     * @param message The current message being processed
     * @return the result of the evaluation
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression
     *             and 'failIfNull is set to true.
     */
    @Deprecated
    Object evaluate(String expression, MuleMessage message) throws ExpressionRuntimeException;

    Object evaluate(String expression, MuleEvent event) throws ExpressionRuntimeException;

    /**
     * Evaluates the given expression. The expression should be a single expression definition with or without
     * enclosing braces. i.e. "context:serviceName" and "#[context:serviceName]" are both valid. For
     * situations where one or more expressions need to be parsed within a single text, the
     * {@link #parse(String,org.mule.api.MuleMessage,boolean)} method should be used since it will iterate
     * through all expressions in a string.
     * 
     * @param expression a single expression i.e. xpath://foo
     * @param message The current message being processed
     * @param failIfNull determines if an exception should be thrown if expression could not be evaluated or
     *            returns null. @return the result of the evaluation
     * @return the parsered expression string
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression
     *             and 'failIfNull is set to true.
     */
    @Deprecated
    Object evaluate(String expression, MuleMessage message, boolean failIfNull)
        throws ExpressionRuntimeException;

    Object evaluate(String expression, MuleEvent event, boolean failIfNull)
        throws ExpressionRuntimeException;

    /**
     * Evaluates the given expression. The expression should be a single expression definition with or without
     * enclosing braces. i.e. "context:serviceName" and "#[context:serviceName]" are both valid. For
     * situations where one or more expressions need to be parsed within a single text, the
     * {@link #parse(String,org.mule.api.MuleMessage,boolean)} method should be used since it will iterate
     * through all expressions in a string.
     * 
     * @param expression one or more expressions embedded in a literal string i.e.
     *            "Value is #[xpath://foo] other value is #[header:foo]."
     * @param evaluator the evaluator to use when executing the expression
     * @param message The current message being processed
     * @param failIfNull determines if an exception should be thrown if expression could not be evaluated or
     *            returns null. @return the result of the evaluation
     * @return the parsered expression string
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression
     *             and 'failIfNull is set to true.
     */
    Object evaluate(String expression, String evaluator, MuleMessage message, boolean failIfNull)
        throws ExpressionRuntimeException;

    /**
     * Evaluates the given expression propagating dataType.
     *
     * <p/>
     * The expression should be a single expression definition with or without
     * enclosing braces. i.e. "context:serviceName" and "#[context:serviceName]" are both valid. For
     * situations where one or more expressions need to be parsed within a single text, the
     * {@link #parse(String, org.mule.api.MuleMessage, boolean)} method should be used since it will iterate
     * through all expressions in a string.
     *
     * @param expression one or more expressions embedded in a literal string i.e.
     *            "Value is #[xpath://foo] other value is #[header:foo]."
     * @param evaluator the evaluator to use when executing the expression
     * @param message The current message being processed
     * @param failIfNull determines if an exception should be thrown if expression could not be evaluated or
     *            returns null. @return the result of the evaluation
     * @return the result of evalauting the expression with the corresponding dataType
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression
     *             and 'failIfNull is set to true.
     */
    TypedValue evaluateTyped(String expression, String evaluator, MuleMessage message, boolean failIfNull)
            throws ExpressionRuntimeException;

    /**
     * Evaluates the given expression resolving the result of the evaluation to a boolean. The expression
     * should be a single expression definition with or without enclosing braces. i.e. "context:serviceName"
     * and "#[context:serviceName]" are both valid.
     * 
     * @param expression a single expression i.e. header:foo=bar
     * @param evaluator the evaluator to use when executing the expression
     * @param message The current message being processed
     */
    boolean evaluateBoolean(String expression, String evaluator, MuleMessage message)
        throws ExpressionRuntimeException;

    /**
     * Evaluates the given expression resolving the result of the evaluation to a boolean. The expression
     * should be a single expression definition with or without enclosing braces. i.e. "context:serviceName"
     * and "#[context:serviceName]" are both valid.
     * 
     * @param expression a single expression i.e. header:foo=bar
     * @param message The current message being processed
     */
    @Deprecated
    boolean evaluateBoolean(String expression, MuleMessage message) throws ExpressionRuntimeException;

    boolean evaluateBoolean(String expression, MuleEvent event) throws ExpressionRuntimeException;

    /**
     * Evaluates the given expression resolving the result of the evaluation to a boolean. The expression
     * should be a single expression definition with or without enclosing braces. i.e. "context:serviceName"
     * and "#[context:serviceName]" are both valid.
     * 
     * @param expression a single expression i.e. header:foo=bar
     * @param evaluator the evaluator to use when executing the expression
     * @param message The current message being processed
     * @param nullReturnsTrue determines if true should be returned if the result of the evaluation is null
     * @param nonBooleanReturnsTrue determines if true should returned if the result is not null but isn't
     *            recognised as a boolean
     */
    boolean evaluateBoolean(String expression,
                                   String evaluator,
                                   MuleMessage message,
                                   boolean nullReturnsTrue,
                                   boolean nonBooleanReturnsTrue) throws ExpressionRuntimeException;

    /**
     * Evaluates the given expression resolving the result of the evaluation to a boolean. The expression
     * should be a single expression definition with or without enclosing braces. i.e. "context:serviceName"
     * and "#[context:serviceName]" are both valid.
     * 
     * @param expression a single expression i.e. header:foo=bar
     * @param message The current message being processed
     * @param nullReturnsTrue determines if true should be returned if the result of the evaluation is null
     * @param nonBooleanReturnsTrue determines if true should returned if the result is not null but isn't
     *            recognised as a boolean
     */
    @Deprecated
    boolean evaluateBoolean(String expression,
                                   MuleMessage message,
                                   boolean nullReturnsTrue,
                                   boolean nonBooleanReturnsTrue) throws ExpressionRuntimeException;

    boolean evaluateBoolean(String expression,
                                   MuleEvent event,
                                   boolean nullReturnsTrue,
                                   boolean nonBooleanReturnsTrue) throws ExpressionRuntimeException;

    /**
     * Enriches the current message using
     * 
     * @param expression a single expression i.e. header://foo that defines how the message should be enriched
     * @param message The current message being processed that will be enriched
     * @param object The object that will be used to enrich the message
     */
    @Deprecated
    void enrich(String expression, MuleMessage message, Object object);

    /**
     * Enriches the current message using a typed value
     *
     * @param expression a single expression i.e. header://foo that defines how the message should be enriched
     * @param message The current message being processed that will be enriched
     * @param object The typed value that will be used to enrich the message
     */
    void enrichTyped(String expression, MuleMessage message, TypedValue object);

    void enrich(String expression, MuleEvent message, Object object);

    /**
     * Enriches the current message
     * 
     * @param expression a single expression i.e. header://foo that defines how the message shoud be enriched
     * @param enricher the enricher to use when executing the expression
     * @param message The current message being processed that will be enriched
     * @param object The object that will be used to enrich the message
     */
    void enrich(String expression, String enricher, MuleMessage message, Object object);

    /**
     * Evaluates expressions in a given string. This method will iterate through each expression and evaluate
     * it. If a user needs to evaluate a single expression they can use
     * {@link #evaluate(String,org.mule.api.MuleMessage,boolean)}.
     * 
     * @param expression one or more expressions ebedded in a literal string i.e.
     *            "Value is #[xpath://foo] other value is #[header:foo]."
     * @param message The current message being processed
     * @return the result of the evaluation
     * @throws org.mule.api.expression.ExpressionRuntimeException if the expression is invalid, or a null is
     *             found for the expression and 'failIfNull is set to true.
     */
    @Deprecated
    String parse(String expression, MuleMessage message) throws ExpressionRuntimeException;

    String parse(String expression, MuleEvent event) throws ExpressionRuntimeException;

    /**
     * Evaluates expressions in a given string. This method will iterate through each expression and evaluate
     * it. If a user needs to evaluate a single expression they can use
     * {@link #evaluate(String,org.mule.api.MuleMessage,boolean)}.
     * 
     * @param expression one or more expressions ebedded in a literal string i.e.
     *            "Value is #[xpath://foo] other value is #[header:foo]."
     * @param message The current message being processed
     * @param failIfNull determines if an exception should be thrown if expression could not be evaluated or
     *            returns null. @return the result of the evaluation
     * @return the parsered expression string
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression
     *             and 'failIfNull is set to true.
     */
    @Deprecated
    String parse(final String expression, final MuleMessage message, final boolean failIfNull)
        throws ExpressionRuntimeException;

    String parse(final String expression, final MuleEvent event, final boolean failIfNull)
        throws ExpressionRuntimeException;

    /**
     * Clears all registered evaluators from the manager.
     */
    void clearEvaluators();

    /**
     * Clears all registered enrichers from the manager.
     */
    void clearEnrichers();

    /**
     * Determines if the expression is valid or not. This method will validate a single expression or
     * expressions embedded in a string. The expression must either be a well formed expression evaluator i.e.
     * #[bean:user] or must be a valid expression language expression.
     * 
     * @param expression the expression to validate
     * @return true if the expression evaluator is recognised
     */
    boolean isValidExpression(String expression);

    /**
     * Determines if the expression is valid or not. This method will validate a single expression or
     * expressions embedded in a string. The expression must either be a well formed expression evaluator i.e.
     * #[bean:user] or must be a valid expression language expression.
     * 
     * @param expression the expression to validate
     * @throws InvalidExpressionException if the expression is invalid, including information about the
     *             position and fault
     * @since 3.0
     */
    void validateExpression(String expression) throws InvalidExpressionException;

    /**
     * Determines if the string is an expression. This method will validate that the string contains either
     * the expression prefix (malformed) or a full expression. This isn't full proof but catches most error
     * cases
     * 
     * @param string is this string an expression string
     * @return true if the string contains an expression
     * @since 3.0
     */
    boolean isExpression(String string);

    TypedValue evaluateTyped(String expression, MuleMessage message);
}
