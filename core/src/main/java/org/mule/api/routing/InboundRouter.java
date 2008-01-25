/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.routing;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;

/**
 * <code>InboundRouter</code> defines an interface for an inbound Message
 * router. An inbound router is used to control how events are received by a
 * service. One or more of these routers can be associated with a
 * InboundRouterCollection implementation.
 * 
 * @see InboundRouterCollection
 */

public interface InboundRouter extends Router
{
    /**
     * A received MuleEvent is passed to this method for processing. The router can
     * control processing by either 1. passing back a null to indicate that the
     * router has either discarded the event of the event has been stored for further
     * processing. A reaosn for storing the event might be that other events in it's
     * correlation group are expected to be received. 2. Pass back an array of one or
     * more events to be processed by the service. Often 1 event is returned, i.e.
     * in the case of event aggregation. The router may return an array of events if
     * a set of events have been resequenced or multiple events have been generated
     * from a single event.
     * 
     * @param event the event received by the inbound endpoint before it is passed to
     *            the service
     * @return null to indicate the event has been stored/destroyed or an array of
     *         events to be processed by the service
     * @throws MessagingException if an error occurs during processing of the event
     */
    MuleEvent[] process(MuleEvent event) throws MessagingException;

    /**
     * Determines if the event should be processed by this router. Routers can be
     * selectively invoked by configuing a filter on them. Usually the filter is
     * applied to the event when calling this method. All core Mule inbound routers
     * extend the SelectiveConsumer router.
     * 
     * @param event the current event to evaluate
     * @return true if the event should be processed by this router
     * @throws MessagingException if the event cannot be evaluated
     * @see org.mule.routing.inbound.SelectiveConsumer
     */
    boolean isMatch(MuleEvent event) throws MessagingException;
}
