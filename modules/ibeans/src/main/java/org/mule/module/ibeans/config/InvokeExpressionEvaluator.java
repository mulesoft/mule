/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.config;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.config.i18n.CoreMessages;

import java.util.Map;

import org.apache.commons.beanutils.MethodUtils;

/**
 * TODO
 */
public class InvokeExpressionEvaluator implements ExpressionEvaluator
{
    public Object evaluate(String expression, MuleMessage message)
    {
        int i = expression.indexOf(".");
        String property;
        String method;
        if(i > -1)
        {
            property = expression.substring(0, i);
            method = expression.substring(i+1);
        }
        else
        {
            throw new IllegalArgumentException();
        }
        Object[] args;

        if(message.getPayload() instanceof Map)
        {
            args = ((Map)message.getPayload()).values().toArray(new Object[]{});
        }
        else if(message.getPayload().getClass().isArray())
        {
            args = (Object[]) message.getPayload();
        }
        else
        {
            args = new Object[]{message.getPayload()};
        }
        Object o = message.getInvocationProperty(property,null);
        if(o!=null)
        {
            try
            {
                return MethodUtils.invokeMethod(o, method, args);
            }
            catch (Exception e)
            {
                throw new ExpressionRuntimeException(CoreMessages.failedToInvoke(expression), e);
            }
        }
        else
        {
            throw new ExpressionRuntimeException(CoreMessages.expressionMalformed(expression, getName()));
        }
    }

    public void setName(String name)
    {
        throw new UnsupportedOperationException("setName");
    }

    public String getName()
    {
        return "invoke";
    }
}
