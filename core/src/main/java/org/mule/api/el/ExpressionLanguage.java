/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.el;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.expression.InvalidExpressionException;
import org.mule.transformer.types.TypedValue;

import java.util.Map;

/**
 * Allows for the execution of expressions within Mule using an expression language. Expression language
 * implementations should not only wrap an expression language runtime but populate the context such that
 * expression can be used to access, and optionally mutate, both Mule configuration and the current message
 * being processed via expression variables. Runtime exceptions should be caught and wrapped in a Mule
 * {@link ExpressionRuntimeException} before being re-thrown.
 * 
 * @since 3.3.0
 */
public interface ExpressionLanguage
{

    /**
     * Execute the expression returning the result. The expression will be executed by the expression language
     * implementation without making any event or message context available.
     * 
     * @param <T> the return type expected
     * @param expression the expression to be executed
     * @return the result of execution of the expression.
     */
    <T> T evaluate(String expression);

    /**
     * Execute the expression returning the result. The expression will be executed by the expression language
     * implementation without making any event or message context available. A Map of variables can be
     * provided that will be able to the expression when executed. Variable provided in the map will only be
     * available if there are no conflicts with context variables provided by the expression language
     * implementation.
     * 
     * @param <T> the return type expected
     * @param expression the expression to be executed
     * @param vars a map of expression variables
     * @return the result of execution of the expression.
     */
    <T> T evaluate(String expression, Map<String, Object> vars);

    /**
     * Execute the expression returning the result. The expression will be executed with MuleEvent context,
     * meaning the expression language implementation should provided access to the message.
     * 
     * @param <T> the return type expected
     * @param expression the expression to be executed
     * @param event the current event being processed
     * @return the result of execution of the expression.
     */
    <T> T evaluate(String expression, MuleEvent event);

    /**
     * Execute the expression returning the result. The expression will be executed with MuleEvent context,
     * meaning the expression language implementation should provided access to the message. A Map of
     * variables can be provided that will be able to the expression when executed. Variable provided in the
     * map will only be available if there are no conflict with context variables provided by the expression
     * language implementation.
     * 
     * @param <T> the return type expected
     * @param expression the expression to be executed
     * @param event the current event being processed
     * @param vars a map of expression variables
     * @return the result of execution of the expression.
     */
    <T> T evaluate(String expression, MuleEvent event, Map<String, Object> vars);

    /**
     * Validates the expression returning true is the expression is valid, false otherwise.. All implementors
     * should should validate expression syntactically. Semantic validation is optional.
     * 
     * @param expression
     * @return
     */
    boolean isValid(String expression);

    /**
     * Validates the expression returning. An {@link InvalidExpressionException} will be thrown is the All
     * implementors should should validate expression syntactically. Semantic validation is optional.
     * 
     * @param expression
     * @return
     */
    void validate(String expression) throws InvalidExpressionException;

    /**
     * Required to provide gradual migration to use of MuleEvent signatures
     * 
     * @see ExpressionLanguage#evaluate(String, MuleEvent)
     */
    @Deprecated
    <T> T evaluate(String expression, MuleMessage message);

    /**
     * Required to provide gradual migration to use of MuleEvent signatures
     * 
     * @see ExpressionLanguage#evaluate(String, MuleEvent)
     */
    @Deprecated
    <T> T evaluate(String expression, MuleMessage message, Map<String, Object> vars);

    /**
     * Enriches a message
     *
     * @param expression a single expression i.e. header://foo that defines how the message should be enriched
     * @param message The message to be enriched
     * @param value The typed value used for enrichment
     */
    void enrich(String expression, MuleMessage message, TypedValue value);

    TypedValue evaluateTyped(String expression, MuleMessage message);
}
