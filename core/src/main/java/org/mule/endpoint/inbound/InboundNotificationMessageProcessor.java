/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.inbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.transport.AbstractConnector;
import org.mule.util.ObjectUtils;

/**
 * Publishes a {@link EndpointMessageNotification}'s when a message is received.
 */
public class InboundNotificationMessageProcessor implements MessageProcessor
{
    protected InboundEndpoint endpoint;

    public InboundNotificationMessageProcessor(InboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        AbstractConnector connector = (AbstractConnector) endpoint.getConnector();
        if (connector.isEnableMessageEvents())
        {
            connector.fireNotification(new EndpointMessageNotification(event.getMessage(), endpoint,
                event.getFlowConstruct(), EndpointMessageNotification.MESSAGE_RECEIVED));
        }

        return event;
    }

    /**
     * @return underlying {@link InboundEndpoint}
     */
    public final InboundEndpoint getInboundEndpoint()
    {
        return this.endpoint;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
