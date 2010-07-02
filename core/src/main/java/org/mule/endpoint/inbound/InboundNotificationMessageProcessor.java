/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.inbound;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.processor.AbstractMessageObserver;
import org.mule.transport.AbstractConnector;

/**
 * Publishes a {@link EndpointMessageNotification}'s when a message is received.
 */
public class InboundNotificationMessageProcessor extends AbstractMessageObserver
{
    protected InboundEndpoint endpoint;

    public InboundNotificationMessageProcessor(InboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    @Override
    public void observe(MuleEvent event)
    {
        AbstractConnector connector = (AbstractConnector) endpoint.getConnector();
        if (connector.isEnableMessageEvents())
        {
            connector.fireNotification(new EndpointMessageNotification(event.getMessage(), endpoint,
                event.getFlowConstruct().getName(), EndpointMessageNotification.MESSAGE_RECEIVED));
        }
    }
}
