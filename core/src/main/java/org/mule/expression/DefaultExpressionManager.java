/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.transport.MessageAdapter;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.TemplateParser;

import java.util.Iterator;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides universal access for evaluating expressions embedded in Mule configurations, such  as Xml, Java,
 * scripting and annotations.
 * <p/>
 * Users can register or unregister {@link ExpressionEvaluator} through this interface.
 */
public class DefaultExpressionManager implements ExpressionManager
{

    /**
     * logger used by this class
     */
    protected static transient final Log logger = LogFactory.getLog(DefaultExpressionManager.class);

    // default style parser
    private TemplateParser parser = TemplateParser.createMuleStyleParser();

    private ConcurrentMap evaluators = new ConcurrentHashMap(8);

    public void registerEvaluator(ExpressionEvaluator evaluator)
    {
        if (evaluator == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("evaluator").getMessage());
        }

        final String name = evaluator.getName();
        // TODO MULE-3809 Eliminate duplicate evaluators registration
        if (logger.isDebugEnabled())
        {
            logger.debug("Evaluators already contain an object named '" + name + "'.  The previous object will be overwritten.");
        }
        evaluators.put(evaluator.getName(), evaluator);
    }

    /**
     * Checks whether an evaluator is registered with the manager
     *
     * @param name the name of the expression evaluator
     * @return true if the evaluator is registered with the manager, false otherwise
     */
    public boolean isEvaluatorRegistered(String name)
    {
        return evaluators.containsKey(name);
    }

    /**
     * Removes the evaluator with the given name
     *
     * @param name the name of the evaluator to remove
     */
    public ExpressionEvaluator unregisterEvaluator(String name)
    {
        if (name == null)
        {
            return null;
        }

        ExpressionEvaluator evaluator = (ExpressionEvaluator)evaluators.remove(name);
        if (evaluator instanceof Disposable)
        {
            ((Disposable) evaluator).dispose();
        }
        return evaluator;
    }

    /**
     * Evaluates the given expression.  The expression should be a single expression definition with or without
     * enclosing braces. i.e. "mule:serviceName" and "#[mule:serviceName]" are both valid. For situations where
     * one or more expressions need to be parsed within a single text, the {@link #parse(String,org.mule.api.transport.MessageAdapter,boolean)}
     * method should be used since it will iterate through all expressions in a string.
     *
     * @param expression a single expression i.e. xpath://foo
     * @param message
     * @return the result of the evaluation
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and
     *                                    'failIfNull is set to true.
     */
    public Object evaluate(String expression, MessageAdapter message) throws ExpressionRuntimeException
    {
        return evaluate(expression, message, false);
    }

    /**
     * Evaluates the given expression.  The expression should be a single expression definition with or without
     * enclosing braces. i.e. "mule:serviceName" and "#[mule:serviceName]" are both valid. For situations where
     * one or more expressions need to be parsed within a single text, the {@link #parse(String,org.mule.api.transport.MessageAdapter,boolean)}
     * method should be used since it will iterate through all expressions in a string.
     *
     * @param expression a single expression i.e. xpath://foo
     * @param message
     *@param failIfNull determines if an exception should be thrown if expression could not be evaluated or returns
     *                   null. @return the result of the evaluation
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and
     *                                    'failIfNull is set to true.
     */
    public Object evaluate(String expression, MessageAdapter message, boolean failIfNull) throws ExpressionRuntimeException
    {
        String name;

        if (expression == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("expression").getMessage());
        }
        if (expression.startsWith(DEFAULT_EXPRESSION_PREFIX))
        {
            expression = expression.substring(2, expression.length() - 1);
        }
        int i = expression.indexOf(":");
        if (i > -1)
        {
            name = expression.substring(0, i);
            expression = expression.substring(i + DEFAULT_EXPRESSION_POSTFIX.length());
        }
        else
        {
            name = expression;
            expression = null;
        }
        return evaluate(expression, name, message, failIfNull);
    }

    /**
     * Evaluates the given expression.  The expression should be a single expression definition with or without
     * enclosing braces. i.e. "mule:serviceName" and "#[mule:serviceName]" are both valid. For situations where
     * one or more expressions need to be parsed within a single text, the {@link #parse(String,org.mule.api.transport.MessageAdapter,boolean)}
     * method should be used since it will iterate through all expressions in a string.
     *
     * @param expression a single expression i.e. xpath://foo
     * @param evaluator  the evaluator to use when executing the expression
     * @param message
     *@param failIfNull determines if an exception should be thrown if expression could not be evaluated or returns
     *                   null. @return the result of the evaluation
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and
     *                                    'failIfNull is set to true.
     */
    public Object evaluate(String expression, String evaluator, MessageAdapter message, boolean failIfNull) throws ExpressionRuntimeException
    {
        ExpressionEvaluator extractor = (ExpressionEvaluator) evaluators.get(evaluator);
        if (extractor == null)
        {
            throw new IllegalArgumentException(CoreMessages.expressionEvaluatorNotRegistered(evaluator).getMessage());
        }
        Object result = extractor.evaluate(expression, message);
        if (result == null && failIfNull)
        {
            throw new ExpressionRuntimeException(CoreMessages.expressionEvaluatorReturnedNull(evaluator, expression));
        }
        return result;
    }


    /**
     * Evaluates expressions in a given string. This method will iterate through each expression and evaluate it. If
     * a user needs to evaluate a single expression they can use {@link #evaluate(String,org.mule.api.transport.MessageAdapter,boolean)}.
     *
     * @param expression a single expression i.e. xpath://foo
     * @param message
     * @return the result of the evaluation
     * @throws org.mule.api.expression.ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and
     *                                    'failIfNull is set to true.
     */
    public String parse(String expression, MessageAdapter message) throws ExpressionRuntimeException
    {
        return parse(expression, message, false);
    }

    /**
     * Evaluates expressions in a given string. This method will iterate through each expression and evaluate it. If
     * a user needs to evaluate a single expression they can use {@link #evaluate(String,org.mule.api.transport.MessageAdapter,boolean)}.
     *
     * @param expression a single expression i.e. xpath://foo
     * @param message
     *@param failIfNull determines if an exception should be thrown if expression could not be evaluated or returns
     *                   null. @return the result of the evaluation
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and
     *                                    'failIfNull is set to true.
     */
    public String parse(final String expression, final MessageAdapter message, final boolean failIfNull) throws ExpressionRuntimeException
    {
        return parser.parse(new TemplateParser.TemplateCallback()
        {
            public Object match(String token)
            {
                return evaluate(token, message, failIfNull);
            }
        }, expression);
    }

    /**
     * Clears all registered evaluators from the manager.
     */
    public synchronized void clearEvaluators()
    {
        for (Iterator iterator = evaluators.values().iterator(); iterator.hasNext();)
        {
            ExpressionEvaluator evaluator = (ExpressionEvaluator) iterator.next();
            if (evaluator instanceof Disposable)
            {
                ((Disposable) evaluator).dispose();
            }
        }
        evaluators.clear();
    }

    /**
     * Determines if the expression is valid or not.  This method will validate a single expression or
     * expressions embedded in a string.  the expression must be well formed i.e. #[bean:user]
     *
     * @param expression the expression to validate
     * @return true if the expression evaluator is recognised
     */
    public boolean isValidExpression(String expression)
    {
        final AtomicBoolean valid = new AtomicBoolean(true);
        final AtomicBoolean match = new AtomicBoolean(false);
        final StringBuffer message = new StringBuffer();
        parser.parse(new TemplateParser.TemplateCallback()
        {
            public Object match(String token)
            {
                match.set(true);
                if (token.indexOf(":") == -1)
                {
                    if (valid.get())
                    {
                        valid.compareAndSet(true, false);
                    }
                    message.append(token).append(" is malformed\n");
                }
                return null;
            }
        }, expression);

        if (message.length() > 0)
        {
            logger.warn("Expression " + expression + " is malformed: " + message.toString());
        }
        return match.get() && valid.get();
    }
}
