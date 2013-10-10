/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.keygenerator;

import org.mule.api.MuleEvent;
import org.mule.api.MuleEventKeyGenerator;

import java.io.NotSerializableException;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implements {@link org.mule.api.MuleEventKeyGenerator} using the Mule expression language to
 * generate the cache keys.
 */
public class ExpressionMuleEventKeyGenerator implements MuleEventKeyGenerator
{

    protected Log logger = LogFactory.getLog(getClass());

    private String expression;

    public Serializable generateKey(MuleEvent event) throws NotSerializableException
    {
        Object key = event.getMuleContext().getExpressionManager().evaluate(expression, event);

        if (logger.isDebugEnabled())
        {
            logger.debug("Generated key for event: " + event + " key: " + key);
        }

        if (key instanceof Serializable)
        {
            return (Serializable) key;
        }
        else
        {
            throw new NotSerializableException("Generated key must a serializable object but was "
                                               + (key != null ? key.getClass().getName() : "null"));
        }
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }
}
