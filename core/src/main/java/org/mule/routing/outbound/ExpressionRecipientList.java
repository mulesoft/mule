/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    public static final String DEFAULT_SELECTOR_EVALUATOR = "header";
    public static final String DEFAULT_SELECTOR_EXPRESSION = DEFAULT_SELECTOR_PROPERTY;

    protected ExpressionConfig expressionConfig = new ExpressionConfig();

    @Override
    protected List getRecipients(MuleEvent event) throws CouldNotRouteOutboundMessageException
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
            return Arrays.asList(StringUtils.splitAndTrim(msgRecipients.toString(), " ,;:"));
        }
        else if (msgRecipients instanceof List)
        {
            return new ArrayList((List) msgRecipients);
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

    public String getCustomEvaluator()
    {
        return expressionConfig.getCustomEvaluator();
    }

    public void setCustomEvaluator(String customEvaluator)
    {
        expressionConfig.setCustomEvaluator(customEvaluator);
    }

    public String getEvaluator()
    {
        return expressionConfig.getEvaluator();
    }

    public void setEvaluator(String evaluator)
    {
        expressionConfig.setEvaluator(evaluator);
    }
}
