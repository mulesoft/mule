/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.umo.routing;

import org.mule.umo.MessagingException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.endpoint.UMOEndpoint;

import java.util.List;

/**
 * <code>UMOOutboundRouter</code> is used to control outbound routing
 * behaviour for an event. One or more Outbound routers can be associated with
 * an <code>UMOOutboundMessageRouter</code> and will be selected based on the
 * filters set on the individual Outbound Router.
 * 
 * @see UMOOutboundMessageRouter
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOOutboundRouter extends UMORouter
{
    /**
     * Sets a list of UMOEndpoint instances associated with this router
     * @param endpoints a list of UMOEndpoint instances
     */
    void setEndpoints(List endpoints);

    /**
     * Gets a list of UMOEndpoint instances associated with this router
     * @return a list of UMOEndpoint instances
     */
    List getEndpoints();

    /**
     * Adds an endpoint to this router
     * @param endpoint the endpoint to add to the router
     */
    void addEndpoint(UMOEndpoint endpoint);

    /**
     * Removes a specific endpoint from the router
     * @param endpoint the endpoint to remove
     * @return true if the endpoint was removed
     */
    boolean removeEndpoint(UMOEndpoint endpoint);

    /**
     * This method is responsible for routing the Message via the Session. The logic for this method will change
     * for each type of router depending on expected behaviour. For example, a MulticastingRouter might just iterate
     * through the list of assoaciated endpoints sending the message.  Another type of router such as the
     * ExceptionBasedRouter will hit the first endpoint, if it fails try the second, and so on.
     * Most router implementations will extends the FilteringOutboundRouter which implements all the common logic
     * need for a router.
     * @param message the message to send via one or more endpoints on this router
     * @param session the session used to actually send the event
     * @param synchronous whether the invocation process should be synchronous or not
     * @return a result message if any from the invocation.  If the synchronous flag is false a null
     * result will always be returned.
     * @throws MessagingException if any errors occur during the sending of messages
     * @see org.mule.routing.outbound.FilteringOutboundRouter
     * @see org.mule.routing.outbound.ExceptionBasedRouter
     * @see org.mule.routing.outbound.MulticastingRouter
     */
    UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous) throws MessagingException;

    /**
     * Determines if the event should be processed by this router.  Routers can be selectively invoked by configuring
     * a filter on them.  Usually the filter is applied to the message when calling this method. All core Mule outbound
     * routers extend the FilteringOutboundRouter router that handles this method automatically.
     * @param message the current message to evaluate
     * @return true if the event should be processed by this router
     * @throws MessagingException if the event cannot be evaluated
     * @see org.mule.routing.inbound.SelectiveConsumer
     */
    boolean isMatch(UMOMessage message) throws MessagingException;

    UMOTransactionConfig getTransactionConfig();

    void setTransactionConfig(UMOTransactionConfig transactionConfig);

    /**
     * Gets the replyTo endpoint for any outgoing messages. This will then be
     * used by other Mule routers to send replies back for this message.  If the underlying
     * protocol supports replyTo messages, such as Jms, a Jms Destination will be attached to the outbound
     * message
     * @return the replyTo endpoint or null if one has not been set.
     */
    public String getReplyTo();

    /**
     * Sets the replyTo endpoint for any outgoing messages. This will then be
     * used by other Mule routers to send replies back for this message.  If the underlying
     * protocol supports replyTo messages, such as Jms, a Jms Destination will be attached to the outbound
     * message
     * @param replyTo endpoint string to use
     */
    public void setReplyTo(String replyTo);

    /**
     * Determines whether this router supports dynamic endpoint.  i.e. endpoints that are not configured
     * at design time.  Endpoints might be pulled from the message or payload.
     * @return
     */
    public boolean isDynamicEndpoints();

}
