/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing;

import org.mule.impl.MuleMessage;
import org.mule.routing.inbound.EventGroup;
import org.mule.routing.response.ResponseCorrelationAggregator;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.transformer.TransformerException;

import java.util.Iterator;

/**
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TestAggregator extends ResponseCorrelationAggregator
{
    /**
     * This method is invoked if the shouldAggregate method is called and
     * returns true. Once this method returns an aggregated message the event
     * group is removed from the router
     * 
     * @param events the event group for this request
     * @return an aggregated message
     * @throws org.mule.umo.routing.RoutingException if the aggregation fails.
     *             in this scenario the whole event group is removed and passed
     *             to the exception handler for this componenet
     */
    protected UMOMessage aggregateEvents(EventGroup events) throws RoutingException
    {
        StringBuffer buffer = new StringBuffer();
        for (Iterator iterator = events.iterator(); iterator.hasNext();) {
            UMOEvent event = (UMOEvent) iterator.next();
            try {
                buffer.append(event.getTransformedMessageAsString());
            } catch (TransformerException e) {
                throw new RoutingException(event.getMessage(), event.getEndpoint(), e);
            }
        }
        System.out.println("event payload is: " + buffer.toString());
        return new MuleMessage(buffer.toString());
    }
}
