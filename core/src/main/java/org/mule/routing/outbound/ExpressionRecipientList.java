/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExpressionRecipientList extends AbstractRecipientList
{
    public static final String DEFAULT_SELECTOR_PROPERTY = "recipients";
    public static final String DEFAULT_SELECTOR_EVALUATOR = "header";
    public static final String DEFAULT_SELECTOR_EXPRESSION = DEFAULT_SELECTOR_PROPERTY;

    private String expression = DEFAULT_SELECTOR_EXPRESSION;
    private String evaluator = DEFAULT_SELECTOR_EVALUATOR;
    private String customEvaluator;
    private String fullExpression;

    @Override
    protected List getRecipients(MuleEvent event) throws CouldNotRouteOutboundMessageException
    {
        String expr = getFullExpression();
        if (!muleContext.getExpressionManager().isValidExpression(expr))
        {
            throw new CouldNotRouteOutboundMessageException(
                    CoreMessages.expressionInvalidForProperty("expression", expr), event, null);
        }

        Object msgRecipients = muleContext.getExpressionManager().evaluate(expr, event.getMessage());
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
        if (fullExpression == null)
        {
            if (evaluator.equalsIgnoreCase("custom"))
            {
                evaluator = customEvaluator;
            }
            fullExpression = MessageFormat.format("{0}{1}:{2}{3}",
                                                  ExpressionManager.DEFAULT_EXPRESSION_PREFIX,
                                                  evaluator, expression,
                                                  ExpressionManager.DEFAULT_EXPRESSION_POSTFIX);
            logger.debug("Full expression for EndpointSelector is: " + fullExpression);
        }
        return fullExpression;
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }

    public String getCustomEvaluator()
    {
        return customEvaluator;
    }

    public void setCustomEvaluator(String customEvaluator)
    {
        this.customEvaluator = customEvaluator;
    }

    public String getEvaluator()
    {
        return evaluator;
    }

    public void setEvaluator(String evaluator)
    {
        this.evaluator = evaluator;
    }
}
