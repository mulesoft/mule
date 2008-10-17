/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.expression;

import org.mule.api.lifecycle.Disposable;
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
public class ExpressionEvaluatorManager
{

    /**
     * logger used by this class
     */
    protected static transient final Log logger = LogFactory.getLog(ExpressionEvaluatorManager.class);

    public static final String DEFAULT_EXPRESSION_PREFIX = "#[";
    public static final String DEFAULT_EXPRESSION_POSTFIX = "]";

    // default style parser
    private static TemplateParser parser = TemplateParser.createMuleStyleParser();

    private static ConcurrentMap evaluators = new ConcurrentHashMap(8);

    public static void registerEvaluator(ExpressionEvaluator evaluator)
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
    public static boolean isEvaluatorRegistered(String name)
    {
        return evaluators.containsKey(name);
    }

    /**
     * Removes the evaluator with the given name
     *
     * @param name the name of the evaluator to remove
     */
    public static ExpressionEvaluator unregisterEvaluator(String name)
    {
        if (name == null)
        {
            return null;
        }

        ExpressionEvaluator evaluator = (ExpressionEvaluator) ExpressionEvaluatorManager.evaluators.remove(name);
        if (evaluator instanceof Disposable)
        {
            ((Disposable) evaluator).dispose();
        }
        return evaluator;
    }

    /**
     * Evaluates the given expression.  The expression should be a single expression definition with or without
     * enclosing braces. i.e. "mule:serviceName" and "#[mule:serviceName]" are both valid. For situations where
     * one or more expressions need to be parsed within a single text, the {@link #parse(String, Object, boolean)}
     * method should be used since it will iterate through all expressions in a string.
     *
     * @param expression a single expression i.e. xpath://foo
     * @param object     The object (usually {@link org.mule.api.MuleMessage}) to evaluate the expression on.
     * @return the result of the evaluation
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and
     *                                    'failIfNull is set to true.
     */
    public static Object evaluate(String expression, Object object) throws ExpressionRuntimeException
    {
        return evaluate(expression, object, DEFAULT_EXPRESSION_PREFIX, false);
    }

    /**
     * Evaluates the given expression.  The expression should be a single expression definition with or without
     * enclosing braces. i.e. "mule:serviceName" and "#[mule:serviceName]" are both valid. For situations where
     * one or more expressions need to be parsed within a single text, the {@link #parse(String, Object, boolean)}
     * method should be used since it will iterate through all expressions in a string.
     *
     * @param expression a single expression i.e. xpath://foo
     * @param object     The object (usually {@link org.mule.api.MuleMessage}) to evaluate the expression on.
     * @param failIfNull determines if an exception should be thrown if expression could not be evaluated or returns
     *                   null.
     * @return the result of the evaluation
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and
     *                                    'failIfNull is set to true.
     */
    public static Object evaluate(String expression, Object object, boolean failIfNull) throws ExpressionRuntimeException
    {
        return evaluate(expression, object, DEFAULT_EXPRESSION_PREFIX, failIfNull);
    }

    /**
     * Evaluates the given expression.  The expression should be a single expression definition with or without
     * enclosing braces. i.e. "mule:serviceName" and "#[mule:serviceName]" are both valid. For situations where
     * one or more expressions need to be parsed within a single text, the {@link #parse(String, Object, boolean)}
     * method should be used since it will iterate through all expressions in a string.
     *
     * @param expression a single expression i.e. xpath://foo
     * @param evaluator  the evaluator to use when executing the expression
     * @param object     The object (usually {@link org.mule.api.MuleMessage}) to evaluate the expression on.
     *                   It is unlikely that users will want to change this execpt maybe to use "["  instead.
     * @param failIfNull determines if an exception should be thrown if expression could not be evaluated or returns
     *                   null.
     * @return the result of the evaluation
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and
     *                                    'failIfNull is set to true.
     */
    public static Object evaluate(String expression, String evaluator, Object object, boolean failIfNull) throws ExpressionRuntimeException
    {
        ExpressionEvaluator extractor = (ExpressionEvaluator) evaluators.get(evaluator);
        if (extractor == null)
        {
            throw new IllegalArgumentException(CoreMessages.expressionEvaluatorNotRegistered(evaluator).getMessage());
        }
        Object result = extractor.evaluate(expression, object);
        if (result == null && failIfNull)
        {
            throw new ExpressionRuntimeException(CoreMessages.expressionEvaluatorReturnedNull(evaluator, expression));
        }
        return result;
    }

    /**
     * Evaluates the given expression.  The expression should be a single expression definition with or without
     * enclosing braces. i.e. "mule:serviceName" and "#[mule:serviceName]" are both valid. For situations where
     * one or more expressions need to be parsed within a single text, the {@link #parse(String, Object, boolean)}
     * method should be used since it will iterate through all expressions in a string.
     *
     * @param expression       a single expression i.e. xpath://foo
     * @param object           The object (usually {@link org.mule.api.MuleMessage}) to evaluate the expression on.
     * @param expressionPrefix the expression prefix to use. The default is "#[" but any character is valid.
     *                         It is unlikely that users will want to change this except maybe to use "["  instead.
     * @param failIfNull       determines if an exception should be thrown if expression could not be evaluated or returns
     *                         null.
     * @return the result of the evaluation
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and
     *                                    'failIfNull is set to true.
     */
    public static Object evaluate(String expression, Object object, String expressionPrefix, boolean failIfNull) throws ExpressionRuntimeException
    {
        String name;

        if (expression == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("expression").getMessage());
        }
        if (expression.startsWith(expressionPrefix))
        {
            expression = expression.substring(2, expression.length() - 1);
        }
        int i = expression.indexOf(":");
        if (i > -1)
        {
            name = expression.substring(0, i);
            expression = expression.substring(i + 1);
        }
        else
        {
            name = expression;
            expression = null;
        }
        return evaluate(expression, name, object, failIfNull);


    }

    /**
     * Evaluates expressions in a given string. This method will iterate through each expression and evaluate it. If
     * a user needs to evaluate a single expression they can use {@link #evaluate(String, Object, boolean)}.
     *
     * @param expression a single expression i.e. xpath://foo
     * @param object     The object (usually {@link org.mule.api.MuleMessage}) to evaluate the expression on.
     * @return the result of the evaluation
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and
     *                                    'failIfNull is set to true.
     */
    public static String parse(String expression, Object object) throws ExpressionRuntimeException
    {
        return parse(expression, object, false);
    }

    /**
     * Evaluates expressions in a given string. This method will iterate through each expression and evaluate it. If
     * a user needs to evaluate a single expression they can use {@link #evaluate(String, Object, boolean)}.
     *
     * @param expression a single expression i.e. xpath://foo
     * @param object     The object (usually {@link org.mule.api.MuleMessage}) to evaluate the expression on.
     * @param failIfNull determines if an exception should be thrown if expression could not be evaluated or returns
     *                   null.
     * @return the result of the evaluation
     * @throws ExpressionRuntimeException if the expression is invalid, or a null is found for the expression and
     *                                    'failIfNull is set to true.
     */
    public static String parse(final String expression, final Object object, final boolean failIfNull) throws ExpressionRuntimeException
    {
        return parser.parse(new TemplateParser.TemplateCallback()
        {
            public Object match(String token)
            {
                return evaluate(token, object, failIfNull);
            }
        }, expression);
    }

    /**
     * Clears all registered evaluators from the manager.
     */
    public static synchronized void clearEvaluators()
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
    public static boolean isValidExpression(String expression)
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
