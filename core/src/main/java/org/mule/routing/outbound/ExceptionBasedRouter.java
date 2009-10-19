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

import org.mule.DefaultMuleMessage;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.RoutingException;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.TransactionTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <code>ExceptionBasedRouter</code> Will send the current event to the first
 * endpoint that doesn't throw an exception. If all attempted endpoints fail then an
 * exception is thrown. <p/> The router will override the sync/async mode of the
 * endpoint and force the sync mode for all endpoints except the last one.
 */

public class ExceptionBasedRouter extends ExpressionRecipientList
{
    @Override
    public MuleMessage route(MuleMessage message, MuleSession session)
        throws RoutingException
    {
        List recipients = null;
        try
        {
            recipients = getRecipients(message);
        }
        catch (RequiredValueException e)
        {
            // ignore because the recipient list is optional for this router
        }

        if (recipients == null)
        {
            int endpointsCount = endpoints.size();
            recipients = new ArrayList(endpointsCount);
            for (int i = 0; i < endpointsCount; i++)
            {
                recipients.add(getEndpoint(i, message));
            }
        }        
        
        if (recipients == null || recipients.size() == 0)
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), message, null);
        }

        if (enableCorrelation != ENABLE_CORRELATION_NEVER)
        {
            boolean correlationSet = message.getCorrelationId() != null;
            if (correlationSet && (enableCorrelation == ENABLE_CORRELATION_IF_NOT_SET))
            {
                logger.debug("CorrelationId is already set, not setting Correlation group size");
            }
            else
            {
                // the correlationId will be set by the AbstractOutboundRouter
                message.setCorrelationGroupSize(recipients.size());
            }
        }

        MuleMessage result = null;
        OutboundEndpoint endpoint = null;
        MuleMessage request = null;
        boolean success = false;

        for (Iterator iterator = recipients.iterator(); iterator.hasNext();)
        {
            request = new DefaultMuleMessage(message.getPayload(), message, muleContext);
            endpoint = getRecipientEndpoint(request, iterator.next());
            boolean lastEndpoint = !iterator.hasNext();

            if (!lastEndpoint)
            {
                logger.info("Sync mode will be forced for " + endpoint.getEndpointURI()
                            + ", as there are more endpoints available.");
            }

            if (!lastEndpoint || endpoint.isSynchronous())
            {
                try
                {
                    result = send(session, request, endpoint);
                    if (result != null)
                    {
                        result.applyTransformers(endpoint.getResponseTransformers());
                    }
                    if (!exceptionPayloadAvailable(result))
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Successful invocation detected, stopping further processing.");
                        }
                        success = true;
                        break;
                    }
                }
                catch (MuleException e)
                {
                    if(logger.isWarnEnabled())
                    {
                        Throwable t = ExceptionHelper.getRootException(e);
                        logger.warn("Failed to send to endpoint: " + endpoint.getEndpointURI().toString()
                                + ". Error was: " + t + ". Trying next endpoint", t);
                    }
                }
            }
            else
            {
                try
                {
                    dispatch(session, request, endpoint);
                    success = true;
                    break;
                }
                catch (MuleException e)
                {
                    logger.info("Failed to dispatch to endpoint: " + endpoint.getEndpointURI().toString()
                                + ". Error was: " + e.getMessage() + ". Trying next endpoint");
                }
            }
        }

        if (!success)
        {
            throw new CouldNotRouteOutboundMessageException(request, endpoint);
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
    
    @Override
    protected TransactionTemplate createTransactionTemplate(MuleSession session, ImmutableEndpoint endpoint)
    {
        return new TransactionTemplate(endpoint.getTransactionConfig(), null, muleContext);
    }
}
