/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ognl.expression;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.lifecycle.Disposable;
import org.mule.config.i18n.CoreMessages;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ognl.Ognl;
import ognl.OgnlException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An expression evaluator that uses OGNL as the expression language
 */
public class OgnlExpressionEvaluator implements ExpressionEvaluator, Disposable
{
    Map<String, Object> expressions = new ConcurrentHashMap<String, Object>(4);

    public static final String NAME = "ognl";

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(OgnlExpressionEvaluator.class);

    public Object evaluate(String expression, MuleMessage message)
    {
        Object o = expressions.get(expression);
        if(o==null)
        {
            try
            {
                o = Ognl.parseExpression(expression);
                expressions.put(expression,  o);
            }
            catch (OgnlException e)
            {
                throw new ExpressionRuntimeException(CoreMessages.expressionMalformed(expression,  NAME), e);
            }
        }

        try
        {
            return Ognl.getValue(o, message.getPayload());
        }
        catch (OgnlException e)
        {
            //Only log the exceptions so that the behaviour is consistent with the Expression API
            logger.warn(e.getMessage(), e);
            return null;
        }

    }

    public void setName(String name)
    {
        throw new UnsupportedOperationException("setName");
    }

    public String getName()
    {
        return NAME;
    }

    public void dispose()
    {
        expressions.clear();
    }
}
