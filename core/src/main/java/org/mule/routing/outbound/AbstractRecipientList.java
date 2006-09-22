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

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;
import org.mule.umo.routing.RoutingException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <code>AbstractRecipientList</code> is used to dispatch a single event to
 * multiple recipients over the same transport. The recipient endpoints can be
 * configured statically or can be obtained from the message payload.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractRecipientList extends FilteringOutboundRouter
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(AbstractRecipientList.class);

    private Map recipientCache = new ConcurrentHashMap();

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous) throws RoutingException
    {
        List list = getRecipients(message);
        List results = new ArrayList();

        if (enableCorrelation != ENABLE_CORRELATION_NEVER) {
            boolean correlationSet = message.getCorrelationGroupSize() != -1;
            if (correlationSet && (enableCorrelation == ENABLE_CORRELATION_IF_NOT_SET)) {
                logger.debug("CorrelationId is already set, not setting Correlation group size");
            } else {
                // the correlationId will be set by the AbstractOutboundRouter
                message.setCorrelationGroupSize(list.size());
            }
        }

        UMOMessage result = null;
        UMOEndpoint endpoint;

        for (Iterator iterator = list.iterator(); iterator.hasNext();)
        {
            String recipient = (String) iterator.next();
            endpoint = getRecipientEndpoint(message, recipient);

            try {
                if (synchronous) {
                    result = send(session, message, endpoint);
                    if (result != null) {
                        results.add(result.getPayload());
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("No result was returned for sync call to: " + endpoint.getEndpointURI());
                        }
                    }
                } else {
                    dispatch(session, message, endpoint);
                }
            } catch (UMOException e) {
                throw new CouldNotRouteOutboundMessageException(message, endpoint, e);
            }
        }

        if (results.size() == 1) {
            return new MuleMessage(results.get(0), result);
        } else if (results.size() == 0) {
            return null;
        } else {
            return new MuleMessage(results, (result == null ? null : result));
        }
    }

    protected UMOEndpoint getRecipientEndpoint(UMOMessage message, String recipient) throws RoutingException
    {
        UMOEndpointURI endpointUri = null;
        UMOEndpoint endpoint = (UMOEndpoint) recipientCache.get(recipient);
        if (endpoint != null) {
            return endpoint;
        }
        try {
            endpointUri = new MuleEndpointURI(recipient);
            endpoint = MuleEndpoint.getOrCreateEndpointForUri(endpointUri, UMOEndpoint.ENDPOINT_TYPE_SENDER);
        } catch (UMOException e) {
            throw new RoutingException(message, endpoint, e);
        }
        recipientCache.put(recipient, endpoint);
        return endpoint;
    }

    protected abstract CopyOnWriteArrayList getRecipients(UMOMessage message);

    public boolean isDynamicEndpoints() {
        return true;
    }
}
