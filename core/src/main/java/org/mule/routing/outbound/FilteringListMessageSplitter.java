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

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <code>FilteringListMessageSplitter</code> Accepts a List as a message payload
 * then routes list elements as messages over an endpoint where the endpoint's filter
 * accepts the payload.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class FilteringListMessageSplitter extends AbstractMessageSplitter
{
    private static ThreadLocal payloads = new ThreadLocal();
    private static ThreadLocal properties = new ThreadLocal();

    /**
     * Template method can be used to split the message up before the getMessagePart
     * method is called .
     * 
     * @param message the message being routed
     */
    protected void initialise(UMOMessage message)
    {
        if (message.getPayload() instanceof List)
        {
            // get a synchronised cloned list
            CopyOnWriteArrayList payload = new CopyOnWriteArrayList((List)message.getPayload());
            payloads.set(payload);
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
        properties.set(props);
    }

    /**
     * @inheritDocs
     */
    protected UMOMessage getMessagePart(UMOMessage message, UMOEndpoint endpoint)
    {
        CopyOnWriteArrayList payload = (CopyOnWriteArrayList)payloads.get();
        for (int i = 0; i < payload.size(); i++)
        {
            Object object = payload.get(i);
            UMOMessage result = new MuleMessage(object, (Map)properties.get());
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
                payload.remove(i);
                return result;
            }
        }
        return null;
    }
}
