/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.outbound;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mule.impl.MuleMessage;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;

/**
 * <code>FilteringListMessageSplitter</code> Accepts a List as a message
 * payload then routes list elements as messages over an endpoint where the
 * endpoint's filter accepts the payload.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class FilteringListMessageSplitter extends AbstractMessageSplitter
{
    private CopyOnWriteArrayList payload;
    private Map properties;

    /**
     * Template method can be used to split the message up before the
     * getMessagePart method is called .
     *
     * @param message the message being routed
     */
    protected void initialise(UMOMessage message)
    {
        if (message.getPayload() instanceof List) {
            // get a synchronised cloned list
            payload = new CopyOnWriteArrayList((List) message.getPayload());
            if (enableCorrelation != ENABLE_CORRELATION_NEVER) {
                // always set correlation group size, even if correlation id
                // has already been set (usually you don't have group size yet
                // by this point.
                final int groupSize = payload.size();
                logger.debug("java.util.List payload detected, setting correlation group size to " + groupSize);
                message.setCorrelationGroupSize(groupSize);
            }
        } else
        {
            throw new IllegalArgumentException("The payload for this router must be of type java.util.list");
        }
        // Cache the properties here because for some message types getting the
        // properties
        // can be expensive
        properties = message.getProperties();
    }

    /**
     * Retrieves a specific message part for the given endpoint. the message
     * will then be routed via the parovider.
     *
     * @param message  the current message being processed
     * @param endpoint the endpoint that will be used to route the resulting
     *                 message part
     * @return the message part to dispatch
     */
    protected UMOMessage getMessagePart(UMOMessage message, UMOEndpoint endpoint)
    {
        for (int i = 0; i < payload.size(); i++) {
            Object object = payload.get(i);
            UMOMessage result = new MuleMessage(object, new HashMap(properties));
            // If there is no filter assume that the endpoint can accept the
            // message.
            // Endpoints will be processed in order to only the last (if any) of
            // the
            // the endpoints may not have a filter
            if (endpoint.getFilter() == null || endpoint.getFilter().accept(result)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Endpoint filter matched. Routing message over: "
                                 + endpoint.getEndpointURI().toString());
                }
                payload.remove(i);
                return result;
            }
        }
        return null;
    }
}
