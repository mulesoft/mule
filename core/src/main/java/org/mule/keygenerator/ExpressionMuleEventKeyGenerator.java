/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.keygenerator;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventKeyGenerator;
import org.mule.api.context.MuleContextAware;
import org.mule.util.AttributeEvaluator;

import java.io.NotSerializableException;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implements {@link org.mule.api.MuleEventKeyGenerator} using the Mule expression language to
 * generate the cache keys.
 */
public class ExpressionMuleEventKeyGenerator implements MuleEventKeyGenerator, MuleContextAware
{

    protected Log logger = LogFactory.getLog(getClass());

    private AttributeEvaluator attributeEvaluator;

    @Override
    public Serializable generateKey(MuleEvent event) throws NotSerializableException
    {
        Object key = attributeEvaluator.resolveValue(event);

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
        return attributeEvaluator.getRawValue();
    }

    public void setExpression(String expression)
    {
        attributeEvaluator = new AttributeEvaluator(expression);
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        attributeEvaluator.initialize(muleContext.getExpressionManager());
    }
}
