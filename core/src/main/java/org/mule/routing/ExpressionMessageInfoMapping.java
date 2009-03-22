/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.routing.MessageInfoMapping;

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

    public String getMessageId(MuleMessage message)
    {
        return (String) muleContext.getExpressionManager().evaluate(getMessageIdExpression(), message, true);
    }

    public String getCorrelationId(MuleMessage message)
    {
        String id = (String) muleContext.getExpressionManager().evaluate(getCorrelationIdExpression(), message, false);
        if (id == null)
        {
            id = getMessageId(message);
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
