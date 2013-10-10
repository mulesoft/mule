/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
