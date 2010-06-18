/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.processor.AbstractMessageObserver;
import org.mule.transport.AbstractConnector;

/**
 * Publishes a {@link EndpointMessageNotification}'s when a message is sent or
 * dispatched.
 */

public class OutboundNotificationMessageProcessor extends AbstractMessageObserver
{

    private OutboundEndpoint endpoint;

    public OutboundNotificationMessageProcessor(OutboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    @Override
    public void observe(MuleEvent event)
    {
        AbstractConnector connector = (AbstractConnector) endpoint.getConnector();
        if (connector.isEnableMessageEvents())
        {
            String component = null;
            if (event.getService() != null)
            {
                component = event.getService().getName();
            }

            int notificationAction;
            if (event.isSynchronous())
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
    }
}
