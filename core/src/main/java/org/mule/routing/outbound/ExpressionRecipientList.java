/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.config.i18n.CoreMessages;
import org.mule.expression.ExpressionConfig;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExpressionRecipientList extends AbstractRecipientList
{
    public static final String DEFAULT_SELECTOR_PROPERTY = "recipients";
    public static final String DEFAULT_SELECTOR_EXPRESSION = DEFAULT_SELECTOR_PROPERTY;

    protected ExpressionConfig expressionConfig = new ExpressionConfig();

    @Override
    protected List<Object> getRecipients(MuleEvent event) throws CouldNotRouteOutboundMessageException
    {
        String expr = getFullExpression();
        if (!muleContext.getExpressionManager().isValidExpression(expr))
        {
            throw new CouldNotRouteOutboundMessageException(
                    CoreMessages.expressionInvalidForProperty("expression", expr), event, null);
        }

        Object msgRecipients = muleContext.getExpressionManager().evaluate(expr, event);
        if (msgRecipients == null)
        {
            throw new CouldNotRouteOutboundMessageException(
                    CoreMessages.propertyIsNotSetOnEvent(getFullExpression()), event, null);
        }
        else if (msgRecipients instanceof String)
        {
            Object[] recipients = StringUtils.splitAndTrim(msgRecipients.toString(), " ,;:");
            return Arrays.asList(recipients);
        }
        else if (msgRecipients instanceof List)
        {
            return new ArrayList<Object>((List<?>) msgRecipients);
        }
        else
        {
            logger.error("Recipients on message are neither String nor List but: " + msgRecipients.getClass());
            throw new CouldNotRouteOutboundMessageException(
                    CoreMessages.propertyIsNotSupportedType(getFullExpression(), new Class[]{String.class, List.class}, msgRecipients.getClass()), event, null);
        }
    }

    public String getFullExpression()
    {
        return expressionConfig.getFullExpression(muleContext.getExpressionManager());
    }

    public String getExpression()
    {
        return expressionConfig.getExpression();
    }

    public void setExpression(String expression)
    {
        expressionConfig.setExpression(expression);
    }

}
