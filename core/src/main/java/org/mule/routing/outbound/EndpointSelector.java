/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointNotFoundException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.CoreMessages;
import org.mule.expression.ExpressionConfig;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <code>EndpointSelector</code> selects the outgoing endpoint based on a
 * an expression evaluator  ("header:endpoint" by default).  It will first try to match the
 * endpoint by name and then by address.
 * The targets to use can be set on the router itself or be global endpoint definitions.
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
 *
 * Deprecated from 3.6.0.  This functionality is specific to Services.
 */
@Deprecated
public class EndpointSelector extends FilteringOutboundRouter
{
    public static final String DEFAULT_SELECTOR_EVALUATOR = "header";
    public static final String DEFAULT_SELECTOR_EXPRESSION = "endpoint";

    private String defaultEndpointName;

    private ExpressionConfig expressionConfig = new ExpressionConfig(DEFAULT_SELECTOR_EXPRESSION, DEFAULT_SELECTOR_EVALUATOR, null);

    @Override
    public MuleEvent route(MuleEvent event) throws RoutingException
    {
        MuleMessage message = event.getMessage();

        List<Object> endpoints;
        String endpointName;

        String prop = expressionConfig.getFullExpression(expressionManager);
        if (!expressionManager.isValidExpression(prop))
        {
            throw new CouldNotRouteOutboundMessageException(
                    CoreMessages.expressionInvalidForProperty("expression", prop), event, null);
        }

        Object property = null;
        try
        {
            property = expressionManager.evaluate(prop, event);
        }
        catch (ExpressionRuntimeException e)
        {
            logger.error(e.getMessage());
        }

        if (property == null && getDefaultEndpointName() == null)
        {
            throw new CouldNotRouteOutboundMessageException(
                    CoreMessages.expressionResultWasNull(
                        expressionConfig.getFullExpression(expressionManager)), event, null);
        }
        else if (property == null)
        {
            logger.info("Expression: " + prop + " returned null, using default endpoint: " + getDefaultEndpointName());
            property = getDefaultEndpointName();
        }

        if (property instanceof String)
        {
            endpoints = new ArrayList<Object>(1);
            endpoints.add(property);
        }
        else if (property instanceof List)
        {
            endpoints = (List<Object>) property;
        }
        else
        {
            throw new CouldNotRouteOutboundMessageException(CoreMessages.propertyIsNotSupportedType(
                    expressionConfig.getFullExpression(expressionManager),
                    new Class[]{String.class, List.class}, property.getClass()), event, null);
        }

        List<MuleEvent> results = new ArrayList<MuleEvent>(endpoints.size());

        for (Iterator<Object> iterator = endpoints.iterator(); iterator.hasNext();)
        {
            endpointName = iterator.next().toString();

            if (StringUtils.isEmpty(endpointName))
            {
                throw new CouldNotRouteOutboundMessageException(
                        CoreMessages.objectIsNull("Endpoint Name: " + expressionConfig.getFullExpression(expressionManager)), event, null);
            }
            MessageProcessor ep = null;
            try
            {
                ep = lookupEndpoint(endpointName);
                if (ep == null)
                {
                    throw new CouldNotRouteOutboundMessageException(CoreMessages.objectNotFound("Endpoint",
                            endpointName), event, null);
                }
                MuleEvent result = sendRequest(event, message, ep, true);
                if (result != null && !VoidMuleEvent.getInstance().equals(result))
                {
                    results.add(result);
                }
            }
            catch (MuleException e)
            {
                throw new CouldNotRouteOutboundMessageException(event, ep, e);
            }
        }
        return resultsHandler.aggregateResults(results, event, muleContext);
    }

    protected MessageProcessor lookupEndpoint(String endpointName) throws MuleException
    {
        for (MessageProcessor target : routes)
        {
            if (target instanceof ImmutableEndpoint)
            {
                ImmutableEndpoint ep = (ImmutableEndpoint) target;
                // Endpoint identifier (deprecated)
                if (endpointName.equals(ep.getName()))
                {
                    return target;
                }
                // Global endpoint
                else if (endpointName.equals(ep.getName()))
                {
                    return target;
                }
                else if (endpointName.equals(ep.getEndpointURI().getUri().toString()))
                    {
                        return target;
                    }
            }
        }
        try
        {
            return getMuleContext().getEndpointFactory().getOutboundEndpoint(endpointName);
        }
        catch (MalformedEndpointException e)
        {
            throw new EndpointNotFoundException(CoreMessages.endpointNotFound(endpointName), e);
        }
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

    public String getDefaultEndpointName()
    {
        return defaultEndpointName;
    }

    public void setDefaultEndpointName(String defaultEndpointName)
    {
        this.defaultEndpointName = defaultEndpointName;
    }
}
