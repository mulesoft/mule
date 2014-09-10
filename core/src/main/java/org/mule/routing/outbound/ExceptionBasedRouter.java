/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.VoidMuleEvent;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.expression.ExpressionConfig;
import org.mule.routing.CorrelationMode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <code>ExceptionBasedRouter</code> Will send the current event to the first
 * endpoint that doesn't throw an exception. If all attempted targets fail then an
 * exception is thrown. <p/> The router will override the sync/async mode of the
 * endpoint and force the sync mode for all targets except the last one.
 *
 * Deprecated from 3.6.0.  This functionality is specific to Services.
 */
@Deprecated
public class ExceptionBasedRouter extends ExpressionRecipientList
{

    public ExceptionBasedRouter()
    {
        expressionConfig = new ExpressionConfig(DEFAULT_SELECTOR_EXPRESSION, DEFAULT_SELECTOR_EVALUATOR, null);
    }

    @Override
    public MuleEvent route(MuleEvent event) throws RoutingException
    {
        MuleMessage message = event.getMessage();

        List<Object> recipients = null;
        try
        {
            recipients = getRecipients(event);
        }
        catch (RequiredValueException e)
        {
            // ignore because the recipient list is optional for this router
        }

        if (recipients == null)
        {
            int endpointsCount = routes.size();
            recipients = new ArrayList<Object>(endpointsCount);
            for (int i = 0; i < endpointsCount; i++)
            {
                recipients.add(getRoute(i, event));
            }
        }

        if (recipients.size() == 0)
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), event, null);
        }

        if (enableCorrelation != CorrelationMode.NEVER)
        {
            boolean correlationSet = message.getCorrelationId() != null;
            if (correlationSet && (enableCorrelation == CorrelationMode.IF_NOT_SET))
            {
                logger.debug("CorrelationId is already set, not setting Correlation group size");
            }
            else
            {
                // the correlationId will be set by the AbstractOutboundRouter
                message.setCorrelationGroupSize(recipients.size());
            }
        }

        MuleEvent result = null;
        OutboundEndpoint endpoint = null;
        MuleMessage request = null;
        boolean success = false;

        for (Iterator<Object> iterator = recipients.iterator(); iterator.hasNext();)
        {
            request = new DefaultMuleMessage(message.getPayload(), message, muleContext);
            try
            {
                endpoint = getRecipientEndpoint(request, iterator.next());
                boolean lastEndpoint = !iterator.hasNext();

                // TODO MULE-4476
                if (!lastEndpoint && !endpoint.getExchangePattern().hasResponse())
                {
                    throw new CouldNotRouteOutboundMessageException(
                        MessageFactory.createStaticMessage("The ExceptionBasedRouter does not support asynchronous endpoints, make sure all endpoints on the router are configured as synchronous"), event, endpoint);
                }

                if (endpoint.getExchangePattern().hasResponse())
                {
                    MuleMessage resultMessage = null;
                    result = sendRequest(event, request, endpoint, true);
                    if (result != null && !VoidMuleEvent.getInstance().equals(result))
                    {
                        resultMessage = result.getMessage();
                    }
                    if (resultMessage != null)
                    {
                        resultMessage.applyTransformers(result, endpoint.getResponseTransformers());
                    }
                    if (!exceptionPayloadAvailable(resultMessage))
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Successful invocation detected, stopping further processing.");
                        }
                        success = true;
                        break;
                    }
                }
                else
                {
                    sendRequest(event, request, endpoint, false);
                    success = true;
                    break;
                }
            }
            catch (MuleException e)
            {
                logger.info("Failed to send/dispatch to endpoint: " + endpoint.getEndpointURI().toString()
                            + ". Error was: " + e.getMessage() + ". Trying next endpoint");
            }
        }

        if (!success)
        {
            throw new CouldNotRouteOutboundMessageException(event, endpoint);
        }

        return result;
    }

    /**
     * @param message message to check
     * @return true if there was an exception payload set
     */
    protected boolean exceptionPayloadAvailable(MuleMessage message)
    {
        if (message == null)
        {
            return false;
        }

        final ExceptionPayload exceptionPayload = message.getExceptionPayload();
        if (exceptionPayload != null)
        {
            logger.info("Failure returned, will try next endpoint. Exception payload is: " + exceptionPayload);
            return true;
        }
        else
        {
            return false;
        }
    }

}
