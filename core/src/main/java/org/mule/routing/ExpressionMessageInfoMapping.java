/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
