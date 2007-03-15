/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;
import org.mule.umo.routing.RoutingException;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous)
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

        UMOMessage result = null;
        UMOEndpoint endpoint;
        UMOMessage request;

        for (Iterator iterator = recipients.iterator(); iterator.hasNext();)
        {
            String recipient = (String)iterator.next();
            // Make a copy of the message. Question is do we do a proper clone? in
            // which case there
            // would potentially be multiple messages with the same id...
            request = new MuleMessage(message.getPayload(), message);
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
            catch (UMOException e)
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
            return new MuleMessage(results.get(0), result);
        }
        else
        {
            return new MuleMessage(results, result);
        }
    }

    protected UMOEndpoint getRecipientEndpoint(UMOMessage message, String recipient) throws RoutingException
    {
        UMOEndpointURI endpointUri = null;
        UMOEndpoint endpoint = (UMOEndpoint)recipientCache.get(recipient);

        if (endpoint != null)
        {
            return endpoint;
        }

        try
        {
            endpointUri = new MuleEndpointURI(recipient);
            endpoint = getManagementContext().getRegistry().getOrCreateEndpointForUri(
                    endpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER);
            endpoint.initialise();
        }
        catch (UMOException e)
        {
            throw new RoutingException(message, endpoint, e);
        }

        UMOEndpoint existingEndpoint = (UMOEndpoint)recipientCache.putIfAbsent(recipient, endpoint);
        if (existingEndpoint != null)
        {
            endpoint = existingEndpoint;
        }

        return endpoint;
    }

    protected abstract List getRecipients(UMOMessage message);

    public boolean isDynamicEndpoints()
    {
        return true;
    }

}
