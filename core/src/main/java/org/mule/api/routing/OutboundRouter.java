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
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transaction.TransactionConfig;

import java.util.List;

/**
 * <code>OutboundRouter</code> is used to control outbound routing behaviour for
 * an event. One or more Outbound routers can be associated with an
 * <code>OutboundRouterCollection</code> and will be selected based on the filters
 * set on the individual Outbound Router.
 * 
 * @see OutboundRouterCollection
 */

public interface OutboundRouter extends Router
{
    /**
     * Sets a list of Endpoint instances associated with this router
     * 
     * @param endpoints a list of Endpoint instances
     */
    void setEndpoints(List endpoints);

    /**
     * Gets a list of Endpoint instances associated with this router
     * 
     * @return a list of Endpoint instances
     */
    List getEndpoints();

    /**
     * Adds an endpoint to this router
     * 
     * @param endpoint the endpoint to add to the router
     */
    void addEndpoint(OutboundEndpoint endpoint);

    /**
     * Removes a specific endpoint from the router
     * 
     * @param endpoint the endpoint to remove
     * @return true if the endpoint was removed
     */
    boolean removeEndpoint(OutboundEndpoint endpoint);

    /**
     * This method is responsible for routing the Message via the MuleSession. The logic
     * for this method will change for each type of router depending on expected
     * behaviour. For example, a MulticastingRouter might just iterate through the
     * list of assoaciated endpoints sending the message. Another type of router such
     * as the ExceptionBasedRouter will hit the first endpoint, if it fails try the
     * second, and so on. Most router implementations will extends the
     * FilteringOutboundRouter which implements all the common logic need for a
     * router.
     * 
     * @param message the message to send via one or more endpoints on this router
     * @param session the session used to actually send the event
     * @return a result message if any from the invocation. If the synchronous flag
     *         is false a null result will always be returned.
     * @throws MessagingException if any errors occur during the sending of messages
     * @see org.mule.routing.outbound.FilteringOutboundRouter
     * @see org.mule.routing.outbound.ExceptionBasedRouter
     * @see org.mule.routing.outbound.MulticastingRouter
     *
     * @since 2.1 the synchronous argument has been removed. Instead use the synchronous attribute of the endpoint
     * you are dispatching to.
     */
    MuleMessage route(MuleMessage message, MuleSession session) throws MessagingException;

    /**
     * Determines if the event should be processed by this router. Routers can be
     * selectively invoked by configuring a filter on them. Usually the filter is
     * applied to the message when calling this method. All core Mule outbound
     * routers extend the FilteringOutboundRouter router that handles this method
     * automatically.
     * 
     * @param message the current message to evaluate
     * @return true if the event should be processed by this router
     * @throws MessagingException if the event cannot be evaluated
     * @see org.mule.routing.inbound.SelectiveConsumer
     */
    boolean isMatch(MuleMessage message) throws MessagingException;

    TransactionConfig getTransactionConfig();

    void setTransactionConfig(TransactionConfig transactionConfig);

    /**
     * Gets the replyTo endpoint for any outgoing messages. This will then be used by
     * other Mule routers to send replies back for this message. If the underlying
     * protocol supports replyTo messages, such as Jms, a Jms Destination will be
     * attached to the outbound message
     * 
     * @return the replyTo endpoint or null if one has not been set.
     */
    String getReplyTo();

    /**
     * Sets the replyTo endpoint for any outgoing messages. This will then be used by
     * other Mule routers to send replies back for this message. If the underlying
     * protocol supports replyTo messages, such as Jms, a Jms Destination will be
     * attached to the outbound message
     * 
     * @param replyTo endpoint string to use
     */
    void setReplyTo(String replyTo);

    /**
     * Determines whether this router supports dynamic endpoint. i.e. endpoints that
     * are not configured at design time. Endpoints might be pulled from the message
     * or payload.
     * 
     * @return
     */
    boolean isDynamicEndpoints();

    /**
     * @param name the Endpoint identifier
     * @return the Endpoint or null if the endpointUri is not registered
     */
    OutboundEndpoint getEndpoint(String name);
    
    /**
     * Determines is this router requires a new message copy.
     * 
     * @return
     */
    boolean isRequiresNewMessage();

}
