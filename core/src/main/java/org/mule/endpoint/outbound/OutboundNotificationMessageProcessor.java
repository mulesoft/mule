/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.transport.AbstractConnector;
import org.mule.util.ObjectUtils;

/**
 * Publishes a {@link EndpointMessageNotification}'s when a message is sent or
 * dispatched.
 */

public class OutboundNotificationMessageProcessor implements MessageProcessor
{

    private OutboundEndpoint endpoint;

    public OutboundNotificationMessageProcessor(OutboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        AbstractConnector connector = (AbstractConnector) endpoint.getConnector();
        if (connector.isEnableMessageEvents())
        {
            String component = null;
            if (event.getFlowConstruct() != null)
            {
                component = event.getFlowConstruct().getName();
            }

            int notificationAction;
            if (event.getEndpoint().getExchangePattern().hasResponse())
            {
                notificationAction = EndpointMessageNotification.MESSAGE_SENT;
            }
            else
            {
                notificationAction = EndpointMessageNotification.MESSAGE_DISPATCHED;
            }
            connector.fireNotification(new EndpointMessageNotification(event.getMessage(),
                event.getEndpoint(), component, notificationAction));
        }

        return event;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
