/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.endpoint.inbound;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.context.notification.EndpointMessageNotification;
import org.mule.runtime.core.transport.AbstractConnector;
import org.mule.runtime.core.util.ObjectUtils;

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

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        AbstractConnector connector = (AbstractConnector) endpoint.getConnector();
        if (connector.isEnableMessageEvents(event))
        {
            connector.fireNotification(new EndpointMessageNotification(event.getMessage(), endpoint,
                event.getFlowConstruct(), EndpointMessageNotification.MESSAGE_RECEIVED), event);
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
