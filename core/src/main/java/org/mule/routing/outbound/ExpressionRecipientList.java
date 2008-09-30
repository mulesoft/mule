/*
 * $Id: StaticRecipientList.java 10489 2008-01-23 17:53:38Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.api.MuleMessage;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.util.StringUtils;
import org.mule.util.expression.ExpressionEvaluatorManager;
import org.mule.config.i18n.CoreMessages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <code>StaticRecipientList</code> is used to dispatch a single event to multiple
 * recipients over the same transport. The recipient endpoints for this router can be
 * configured statically on the router itself.
 */

public class ExpressionRecipientList extends AbstractRecipientList
{
    public static final String DEFAULT_SELECTOR_PROPERTY = "recipients";
    public static final String DEFAULT_SELECTOR_EVALUATOR = "header";
    public static final String DEFAULT_SELECTOR_EXPRESSION = DEFAULT_SELECTOR_PROPERTY;

    private String expression = DEFAULT_SELECTOR_EXPRESSION;
    private String evaluator = DEFAULT_SELECTOR_EVALUATOR;
    private String customEvaluator;
    private String fullExpression;

    protected List getRecipients(MuleMessage message) throws CouldNotRouteOutboundMessageException
    {
        String expr = getFullExpression();
        if(!ExpressionEvaluatorManager.isValidExpression(expr))
        {
            throw new CouldNotRouteOutboundMessageException(
                    CoreMessages.expressionInvalidForProperty("expression", expr), message, null);
        }

        Object msgRecipients = ExpressionEvaluatorManager.evaluate(expr, message);
        if(msgRecipients ==null)
        {
            throw new CouldNotRouteOutboundMessageException(
                    CoreMessages.propertyIsNotSetOnEvent(getFullExpression()), message, null);
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
                    CoreMessages.propertyIsNotSupportedType(getFullExpression(), new Class[]{String.class, List.class}, msgRecipients.getClass()), message, null);
        }
    }

    public String getFullExpression()
    {
        if(fullExpression==null)
        {
            if(evaluator.equalsIgnoreCase("custom"))
            {
                evaluator = customEvaluator;
            }
                fullExpression = "${" + evaluator + ":" + expression + "}";
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