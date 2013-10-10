/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
 * Publishes a {@link EndpointMessageNotification}'s when a message is sent or dispatched.
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
                    event.getFlowConstruct(), notificationAction));
        }

        return event;
    }

    public void dispatchNotification(EndpointMessageNotification notification)
    {
        AbstractConnector connector = (AbstractConnector) endpoint.getConnector();
        if (notification != null && connector.isEnableMessageEvents())
        {
            connector.fireNotification(notification);
        }
    }

    public EndpointMessageNotification createBeginNotification(MuleEvent event)
    {
        AbstractConnector connector = (AbstractConnector) endpoint.getConnector();
        if (connector.isEnableMessageEvents())
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
