/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.StringUtils;
import org.mule.util.expression.ExpressionEvaluatorManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <code>EndpointSelector</code> selects the outgoing endpoint based on a
 * an expression evaluator  ("header:endpoint" by default).  It will first try to match the
 * endpoint by name and then by address.
 * The endpoints to use can be set on the router itself or be global endpoint definitions.
 * <pre>
 * <p/>
 * &lt;outbound&gt;
 *      &lt;endpoint-selector-router evaluator="xpath" expression="/MSG/HEADER/NEXT-ADDRESS"&gt;
 *          &lt;endpoint name="dest1" address="jms://queue1" /&gt;
 *          &lt;endpoint name="dest2" address="jms://queue2" /&gt;
 *          &lt;endpoint name="dest3" address="jms://queue3" /&gt;
 *      &lt;/endpoint-selector-router&gt;
 * &lt;/outbound&gt;
 * <p/>
 * </pre>
 */
public class EndpointSelector extends FilteringOutboundRouter
{
    public static final String DEFAULT_SELECTOR_EVALUATOR = "header";
    public static final String DEFAULT_SELECTOR_EXPRESSION = "endpoint";

    private String expression = DEFAULT_SELECTOR_EXPRESSION;
    private String evaluator = DEFAULT_SELECTOR_EVALUATOR;
    private String customEvaluator;
    private String fullExpression;

    public MuleMessage route(MuleMessage message, MuleSession session, boolean synchronous)
            throws RoutingException
    {
        List endpoints;
        String endpointName;

        String prop = getFullExpression();
        if (!ExpressionEvaluatorManager.isValidExpression(prop))
        {
            throw new CouldNotRouteOutboundMessageException(
                    CoreMessages.expressionInvalidForProperty("expression", prop), message, null);
        }

        Object property = ExpressionEvaluatorManager.evaluate(prop, message);
        if (property == null)
        {
            throw new CouldNotRouteOutboundMessageException(
                    CoreMessages.propertyIsNotSetOnEvent(getFullExpression()), message, null);
        }

        if (property instanceof String)
        {
            endpoints = new ArrayList(1);
            endpoints.add(property);
        }
        else if (property instanceof List)
        {
            endpoints = (List) property;
        }
        else
        {
            throw new CouldNotRouteOutboundMessageException(CoreMessages.propertyIsNotSupportedType(
                    getFullExpression(), new Class[]{String.class, List.class}, property.getClass()), message, null);
        }

        MuleMessage result = null;
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            endpointName = iterator.next().toString();

            if (StringUtils.isEmpty(endpointName))
            {
                throw new CouldNotRouteOutboundMessageException(
                        CoreMessages.objectIsNull("Endpoint Name: " + getFullExpression()), message, null);
            }
            OutboundEndpoint ep = null;
            try
            {
                ep = lookupEndpoint(endpointName);
                if (ep == null)
                {
                    throw new CouldNotRouteOutboundMessageException(CoreMessages.objectNotFound("Endpoint",
                            endpointName), message, ep);
                }
                if (synchronous)
                {
                    // TODO See MULE-2613, we only return the last message here
                    result = send(session, message, ep);
                }
                else
                {
                    dispatch(session, message, ep);
                }
            }
            catch (MuleException e)
            {
                throw new CouldNotRouteOutboundMessageException(message, ep, e);
            }
        }
        return result;
    }

    protected OutboundEndpoint lookupEndpoint(String endpointName) throws MuleException
    {
        OutboundEndpoint ep;
        Iterator iterator = endpoints.iterator();
        while (iterator.hasNext())
        {
            ep = (OutboundEndpoint) iterator.next();
            // Endpoint identifier (deprecated)
            if (endpointName.equals(ep.getEndpointURI().getEndpointName()))
            {
                return ep;
            }
            // Global endpoint
            else if (endpointName.equals(ep.getName()))
            {
                return ep;
            }
            else if (endpointName.equals(ep.getEndpointURI().getUri().toString()))
            {
                return ep;
            }
        }
        return getMuleContext().getRegistry().lookupEndpointFactory().getOutboundEndpoint(endpointName);
    }

    public String getFullExpression()
    {
        if (fullExpression == null)
        {
            if (evaluator.equalsIgnoreCase("custom"))
            {
                evaluator = customEvaluator;
            }
            fullExpression = evaluator + ":" + expression;
            logger.debug("Full expression for EndpointSelector is: " + fullExpression);
        }
        return ExpressionEvaluatorManager.DEFAULT_EXPRESSION_PREFIX + fullExpression + ExpressionEvaluatorManager.DEFAULT_EXPRESSION_POSTFIX;
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
