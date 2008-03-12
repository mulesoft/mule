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

    public static final String DEFAULT_EXPRESSION_PREFIX = "${";

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
        
        ExpressionEvaluator evaluator = (ExpressionEvaluator) ExpressionEvaluatorManager.evaluator.remove(name);
        if(evaluator instanceof Disposable)
        {
            ((Disposable) evaluator).dispose();
        }
        return evaluator;
    }

    public static Object evaluate(String expression, Object object) throws ExpressionRuntimeException
    {
        return evaluate(expression, object, DEFAULT_EXPRESSION_PREFIX, false);
    }

    public static Object evaluate(String expression, Object object, boolean failIfNull) throws ExpressionRuntimeException
    {
        return evaluate(expression, object, DEFAULT_EXPRESSION_PREFIX, failIfNull);
    }
    
    public static Object evaluate(String expression, Object object, String expressionPrefix, boolean failIfNull) throws ExpressionRuntimeException
    {
        String name;

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
        else
        {
            name = expression;
            expression = null;
        }
        ExpressionEvaluator extractor = (ExpressionEvaluator) evaluator.get(name);
        if(extractor==null)
        {
            throw new IllegalArgumentException(CoreMessages.expressionEvaluatorNotRegistered(name).getMessage());
        }
        Object result = extractor.evaluate(expression, object);
        if(result==null && failIfNull)
        {
            throw new ExpressionRuntimeException(CoreMessages.expressionEvaluatorReturnedNull(name, expression));
        }
        return result;
    }

    public static synchronized void clearEvaluators()
    {
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

    public static boolean isValidExpression(String expression)
    {
        return isValidExpression(expression, DEFAULT_EXPRESSION_PREFIX);
    }

    public static boolean isValidExpression(String expression, String expressionPrefix)
    {
        if(expression.startsWith(expressionPrefix))
        {
            expression = expression.substring(2, expression.length()-1);
        }
        String name;
        int i = expression.indexOf(":");
        if(i>-1)
        {
            name = expression.substring(0, i);
        }
        else
        {
            name = expression;
        }
        return isEvaluatorRegistered(name);
    }

}
