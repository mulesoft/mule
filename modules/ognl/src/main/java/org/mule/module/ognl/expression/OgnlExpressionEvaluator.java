/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ognl.expression;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.lifecycle.Disposable;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.ognl.config.OGNLNamespaceHandler;

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

    static
    {
        Log deprecationLogger = LogFactory.getLog(OgnlExpressionEvaluator.class);
        deprecationLogger.warn(OGNLNamespaceHandler.getDeprecationWarning());
    }

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
