/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.module.http.api.HttpParameters;

import java.util.Map;


public class HttpMapParam extends HttpParam
{

    private String expression;

    public HttpMapParam(HttpParamType type)
    {
        super(type);
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    @Override
    public void resolve(ParameterMap parameterMap, MuleEvent muleEvent)
    {
        MuleContext muleContext = muleEvent.getMuleContext();
        Object expressionResult = muleContext.getExpressionManager().evaluate(expression, muleEvent);

        if (expressionResult instanceof HttpParameters)
        {
            resolveHttpParameters(parameterMap, (HttpParameters) expressionResult);
        }
        else
        {
            resolveMapObjObj(parameterMap, (Map<Object, Object>) expressionResult);
        }
    }

    private static void resolveHttpParameters(ParameterMap parameterMap, HttpParameters expressionResult)
    {
        HttpParameters expressionHttpParameters = expressionResult;
        for (String key : expressionHttpParameters.keySet())
        {
            for (String value : expressionHttpParameters.getAll(key))
            {
                parameterMap.put(key, value);
            }
        }
    }

    private static void resolveMapObjObj(ParameterMap parameterMap, Map<Object, Object> expressionResult)
    {
        Map<Object, Object> expressionParamMap = expressionResult;

        for (Map.Entry<Object, Object> entry : expressionParamMap.entrySet())
        {
            String paramName = entry.getKey().toString();
            Object paramValue = entry.getValue();

            if (paramValue instanceof Iterable)
            {
                for (Object value : (Iterable) paramValue)
                {
                    parameterMap.put(paramName, toStringIfPossible(value));
                }
            }
            else
            {
                parameterMap.put(paramName, toStringIfPossible(paramValue));
            }
        }
    }

    private static String toStringIfPossible(Object paramValue)
    {
        return paramValue != null ? paramValue.toString() : null;
    }
}
