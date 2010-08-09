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

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transaction.TransactionConfig;
import org.mule.management.stats.RouterStatistics;

import java.util.List;

/**
 * <code>OutboundRouter</code> is used to control outbound routing behaviour for an
 * event. One or more Outbound routers can be associated with an
 * <code>OutboundRouterCollection</code> and will be selected based on the filters
 * set on the individual Outbound Router.
 * 
 * @see OutboundRouterCollection
 */
public interface OutboundRouter extends MatchableMessageRouter, Initialisable, Disposable
{
    /**
     * Sets a list of MessageProcessor instances associated with this router
     * 
     * @param routes a list of MessageProcessor instances
     */
    void setRoutes(List<MessageProcessor> routes);

    /**
     * Gets a list of MessageProcessor instances associated with this router
     * 
     * @return a list of MessageProcessor instances
     */
    List<MessageProcessor> getRoutes();


    TransactionConfig getTransactionConfig();

    void setTransactionConfig(TransactionConfig transactionConfig);

    /**
     * Gets the replyTo route for any outgoing messages. This will then be used by
     * other Mule routers to send replies back for this message. If the underlying
     * protocol supports replyTo messages, such as Jms, a Jms Destination will be
     * attached to the outbound message
     * 
     * @return the replyTo route or null if one has not been set.
     */
    String getReplyTo();

    /**
     * Sets the replyTo route for any outgoing messages. This will then be used by
     * other Mule routers to send replies back for this message. If the underlying
     * protocol supports replyTo messages, such as Jms, a Jms Destination will be
     * attached to the outbound message
     * 
     * @param replyTo route string to use
     */
    void setReplyTo(String replyTo);

    /**
     * Determines whether this router supports dynamic route. i.e. routes that are
     * not configured at design time. routes might be pulled from the message or
     * payload.
     */
    boolean isDynamicRoutes();

    void setRouterStatistics(RouterStatistics stats);

    RouterStatistics getRouterStatistics();
}
