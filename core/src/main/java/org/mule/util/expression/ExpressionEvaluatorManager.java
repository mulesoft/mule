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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Provides universal access for evaluating expressions embedded in Mule configurations, such  as Xml, Java,
 * scripting and annotations.
 *
 * Users can register or unregister {@link ExpressionEvaluator} through this interface.
 * */
public class ExpressionEvaluatorManager
{
    public static final String DEFAULT_EVALUATOR_NAME = MessageHeaderExpressionEvaluator.NAME;

    public static final String DEFAULT_EXPRESSION_PREFIX = "${";

    private static String defaultEvaluator = DEFAULT_EVALUATOR_NAME;

    private static Map evaluator = new HashMap(8);

    public static void registerEvaluator(ExpressionEvaluator extractor)
    {
        if(extractor ==null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("extractor").getMessage());
        }
        if(evaluator.containsKey(extractor.getName()))
        {
            throw new IllegalArgumentException(CoreMessages.objectAlreadyExists(extractor.getName()).getMessage());
        }
        evaluator.put(extractor.getName(), extractor);
    }

    public static boolean isEvaluatorRegistered(String name)
    {
        return evaluator.get(name)!=null;
    }
    public static ExpressionEvaluator unregisterEvaluator(String name)
    {
        if(name==null)
        {
            return null;
        }
        if(name.equals(getDefaultEvaluator()))
        {
            setDefaultEvaluator(DEFAULT_EVALUATOR_NAME);
        }
        ExpressionEvaluator evaluator = (ExpressionEvaluator) ExpressionEvaluatorManager.evaluator.remove(name);
        if(evaluator instanceof Disposable)
        {
            ((Disposable) evaluator).dispose();
        }
        return evaluator;
    }

    public static Object evaluate(String expression, Object object)
    {
        return evaluate(expression, object, DEFAULT_EXPRESSION_PREFIX);
    }
    
    public static Object evaluate(String expression, Object object, String expressionPrefix)
    {
        String name = getDefaultEvaluator();

        if(expression==null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("expression").getMessage());
        }
        if(expression.startsWith(expressionPrefix))
        {
            expression = expression.substring(2, expression.length()-1);
        }
        int i = expression.indexOf(":");
        if(i>-1)
        {
            name = expression.substring(0, i);
            expression = expression.substring(i+1);
        }
        ExpressionEvaluator extractor = (ExpressionEvaluator) evaluator.get(name);
        if(extractor==null)
        {
            throw new IllegalArgumentException(CoreMessages.noExtractorRegisteredWithKey(name).getMessage());
        }
        return extractor.evaluate(expression, object);
    }

    public static String getDefaultEvaluator()
    {
        return defaultEvaluator;
    }

    public static void setDefaultEvaluator(String defaultEvaluator)
    {
        if(evaluator.get(defaultEvaluator)==null)
        {
            throw new IllegalArgumentException(defaultEvaluator);
        }
        ExpressionEvaluatorManager.defaultEvaluator = defaultEvaluator;
    }

    public static synchronized void clearEvaluators()
    {
        defaultEvaluator = DEFAULT_EVALUATOR_NAME;
        for (Iterator iterator = evaluator.values().iterator(); iterator.hasNext();)
        {
            ExpressionEvaluator evaluator = (ExpressionEvaluator)iterator.next();
            if(evaluator instanceof Disposable)
            {
                ((Disposable) evaluator).dispose();
            }
        }
        evaluator.clear();
    }
}
