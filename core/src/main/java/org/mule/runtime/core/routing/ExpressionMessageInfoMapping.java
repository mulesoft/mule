/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.routing.MessageInfoMapping;

/**
 * TODO
 */
public class ExpressionMessageInfoMapping implements MessageInfoMapping, MuleContextAware
{
    private String correlationIdExpression;
    private String messageIdExpression;
    private MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public String getMessageId(MuleEvent event)
    {
        return (String) muleContext.getExpressionManager().evaluate(getMessageIdExpression(), event, true);
    }

    public String getCorrelationId(MuleEvent event)
    {
        String id = (String) muleContext.getExpressionManager().evaluate(getCorrelationIdExpression(), event, false);
        if (id == null)
        {
            id = getMessageId(event);
        }
        return id;
    }

    public String getCorrelationIdExpression()
    {
        return correlationIdExpression;
    }

    public void setCorrelationIdExpression(String correlationIdExpression)
    {
        this.correlationIdExpression = correlationIdExpression;
    }

    public String getMessageIdExpression()
    {
        return messageIdExpression;
    }

    public void setMessageIdExpression(String messageIdExpression)
    {
        this.messageIdExpression = messageIdExpression;
    }
}
