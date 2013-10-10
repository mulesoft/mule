/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.routing;

import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transaction.TransactionConfig;

import java.util.List;

/**
 * <code>OutboundRouter</code> is used to control outbound routing behaviour for an
 * event. One or more Outbound routers can be associated with an
 * <code>OutboundRouterCollection</code> and will be selected based on the filters
 * set on the individual Outbound Router.
 * 
 * @see OutboundRouterCollection
 */
public interface OutboundRouter
    extends MatchableMessageRouter, RouterStatisticsRecorder, Lifecycle, MuleContextAware, FlowConstructAware
{

    void setTransactionConfig(TransactionConfig transactionConfig);

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

    /**
     * Gets a list of MessageProcessor instances associated with this router
     * 
     * @return a list of MessageProcessor instances
     */
    List<MessageProcessor> getRoutes();

}
