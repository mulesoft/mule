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
import org.mule.MuleServer;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.registry.RegistrationException;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutingException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractRecipientList</code> is used to dispatch a single event to
 * multiple recipients over the same transport. The recipient endpoints can be
 * configured statically or can be obtained from the message payload.
 */

public abstract class AbstractRecipientList extends FilteringOutboundRouter
{
    /**
     * logger used by this class
     */
    protected final Log logger = LogFactory.getLog(getClass());

    private final ConcurrentMap recipientCache = new ConcurrentHashMap();

    public MuleMessage route(MuleMessage message, MuleSession session, boolean synchronous)
        throws RoutingException
    {
        List recipients = this.getRecipients(message);
        List results = new ArrayList();

        if (enableCorrelation != ENABLE_CORRELATION_NEVER)
        {
            boolean correlationSet = message.getCorrelationGroupSize() != -1;
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
        OutboundEndpoint endpoint;
        MuleMessage request;

        for (Iterator iterator = recipients.iterator(); iterator.hasNext();)
        {
            Object recipient = iterator.next();
            // Make a copy of the message. Question is do we do a proper clone? in
            // which case there
            // would potentially be multiple messages with the same id...
            request = new DefaultMuleMessage(message.getPayload(), message);
            endpoint = this.getRecipientEndpoint(request, recipient);

            try
            {
                if (synchronous)
                {
                    result = this.send(session, request, endpoint);
                    if (result != null)
                    {
                        results.add(result.getPayload());
                    }
                    else
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("No result was returned for sync call to: "
                                            + endpoint.getEndpointURI());
                        }
                    }
                }
                else
                {
                    this.dispatch(session, request, endpoint);
                }
            }
            catch (MuleException e)
            {
                throw new CouldNotRouteOutboundMessageException(request, endpoint, e);
            }
        }

        if (results.size() == 0)
        {
            return null;
        }
        else if (results.size() == 1)
        {
            return new DefaultMuleMessage(results.get(0), result);
        }
        else
        {
            return new DefaultMuleMessage(results, result);
        }
    }

    protected OutboundEndpoint getRecipientEndpoint(MuleMessage message, Object recipient) throws RoutingException
    {
        OutboundEndpoint endpoint = null;
        try
        {
            if (recipient instanceof EndpointURI)
            {
                endpoint = getRecipientEndpointFromUri((EndpointURI) recipient);
            }
            else if (recipient instanceof String)
            {
                endpoint = getRecipientEndpointFromString(message, (String) recipient);
            }
            if (null == endpoint)
            {
                throw new RegistrationException("Failed to create endpoint for: " + recipient);
            }

            OutboundEndpoint existingEndpoint = (OutboundEndpoint) recipientCache.putIfAbsent(recipient, endpoint);
            if (existingEndpoint != null)
            {
                endpoint = existingEndpoint;
            }
        }
        catch (MuleException e)
        {
            throw new RoutingException(message, endpoint, e);
        }
        return endpoint;
    }

    protected OutboundEndpoint getRecipientEndpointFromUri(EndpointURI uri)
            throws MuleException
    {
        OutboundEndpoint endpoint = null;
        if (null != getMuleContext() && null != getMuleContext().getRegistry())
        {
            endpoint = getMuleContext().getRegistry().lookupEndpointFactory().getOutboundEndpoint(uri.getAddress());
        }
        if (null != endpoint)
        {
            MuleServer.getMuleContext().getLifecycleManager().applyCompletedPhases(endpoint);
        }
        return endpoint;
    }

    protected OutboundEndpoint getRecipientEndpointFromString(MuleMessage message, String recipient)
            throws MuleException
    {
        OutboundEndpoint endpoint = (OutboundEndpoint) recipientCache.get(recipient);
        if (null == endpoint && null != getMuleContext() && null != getMuleContext().getRegistry())
        {
            endpoint = getMuleContext().getRegistry().lookupEndpointFactory().getOutboundEndpoint(recipient);
        }
        return endpoint;
    }

    protected abstract List getRecipients(MuleMessage message);

    public boolean isDynamicEndpoints()
    {
        return true;
    }

}
