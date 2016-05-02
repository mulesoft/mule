/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.endpoint.outbound;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.context.notification.EndpointMessageNotification;
import org.mule.runtime.core.transport.AbstractConnector;
import org.mule.runtime.core.util.ObjectUtils;

/**
 * Publishes a {@link EndpointMessageNotification}'s when a message is sent or dispatched.
 */

public class OutboundNotificationMessageProcessor implements MessageProcessor
{

    private OutboundEndpoint endpoint;

    public OutboundNotificationMessageProcessor(OutboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        AbstractConnector connector = (AbstractConnector) endpoint.getConnector();
        if (connector.isEnableMessageEvents(event))
        {
            int notificationAction;
            if (endpoint.getExchangePattern().hasResponse())
            {
                notificationAction = EndpointMessageNotification.MESSAGE_SEND_END;
            }
            else
            {
                notificationAction = EndpointMessageNotification.MESSAGE_DISPATCH_END;
            }
            dispatchNotification(new EndpointMessageNotification(event.getMessage(), endpoint,
                    event.getFlowConstruct(), notificationAction), event);
        }

        return event;
    }

    public void dispatchNotification(EndpointMessageNotification notification, MuleEvent event)
    {
        AbstractConnector connector = (AbstractConnector) endpoint.getConnector();
        if (notification != null && connector.isEnableMessageEvents(event))
        {
            connector.fireNotification(notification, event);
        }
    }

    public EndpointMessageNotification createBeginNotification(MuleEvent event)
    {
        AbstractConnector connector = (AbstractConnector) endpoint.getConnector();
        if (connector.isEnableMessageEvents(event))
        {
            int notificationAction;
            if (endpoint.getExchangePattern().hasResponse())
            {
                notificationAction = EndpointMessageNotification.MESSAGE_SEND_BEGIN;
            }
            else
            {
                notificationAction = EndpointMessageNotification.MESSAGE_DISPATCH_BEGIN;
            }
            return new EndpointMessageNotification(event.getMessage(), endpoint, event.getFlowConstruct(), notificationAction);
        }

        return null;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
