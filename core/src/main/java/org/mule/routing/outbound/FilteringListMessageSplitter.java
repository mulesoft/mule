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
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <code>FilteringListMessageSplitter</code> accepts a List as a message payload
 * then routes list elements as messages over an endpoint where the endpoint's filter
 * accepts the payload.
 */
public class FilteringListMessageSplitter extends AbstractMessageSplitter
{
    private static final ThreadLocal payloadContext = new ThreadLocal();
    private static final ThreadLocal propertiesContext = new ThreadLocal();

    /**
     * Template method can be used to split the message up before the getMessagePart
     * method is called .
     * 
     * @param message the message being routed
     */
    // @Override
    protected void initialise(UMOMessage message)
    {
        if (message.getPayload() instanceof List)
        {
            // get a copy of the list
            List payload = new LinkedList((List)message.getPayload());
            payloadContext.set(payload);

            if (enableCorrelation != ENABLE_CORRELATION_NEVER)
            {
                // always set correlation group size, even if correlation id
                // has already been set (usually you don't have group size yet
                // by this point.
                final int groupSize = payload.size();
                message.setCorrelationGroupSize(groupSize);
                if (logger.isDebugEnabled())
                {
                    logger.debug("java.util.List payload detected, setting correlation group size to "
                                    + groupSize);
                }
            }
        }
        else
        {
            throw new IllegalArgumentException("The payload for this router must be of type java.util.List");
        }

        // Cache the properties here because for some message types getting the
        // properties can be expensive
        Map props = new HashMap();
        for (Iterator iterator = message.getPropertyNames().iterator(); iterator.hasNext();)
        {
            String propertyKey = (String)iterator.next();
            props.put(propertyKey, message.getProperty(propertyKey));
        }
        propertiesContext.set(props);
    }

    /**
     * @inheritDocs
     */
    protected UMOMessage getMessagePart(UMOMessage message, UMOEndpoint endpoint)
    {
        List payloads = (List)payloadContext.get();

        for (Iterator i = payloads.iterator(); i.hasNext();)
        {
            Object payload = i.next();
            UMOMessage result = new MuleMessage(payload, (Map)propertiesContext.get());
            // If there is no filter assume that the endpoint can accept the
            // message. Endpoints will be processed in order to only the last
            // (if any) of the the endpoints may not have a filter
            if (endpoint.getFilter() == null || endpoint.getFilter().accept(result))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Endpoint filter matched. Routing message over: "
                                    + endpoint.getEndpointURI().toString());
                }
                i.remove();
                return result;
            }
        }

        return null;
    }
}
