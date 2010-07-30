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
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transport.DispatchException;
import org.mule.context.notification.SecurityNotification;
import org.mule.message.DefaultExceptionPayload;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.transport.AbstractConnector;

/**
 * Filters the outbound flow using the {@link EndpointSecurityFilter} configured on
 * the endpoint. If unauthorised the outbound flow is stopped and therefore the
 * message is not send or dispatched by the transport. When unauthorised the request
 * message is returned as the response.
 */

public class OutboundSecurityFilterMessageProcessor extends AbstractInterceptingMessageProcessor
{

    private OutboundEndpoint endpoint;

    public OutboundSecurityFilterMessageProcessor(OutboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        AbstractConnector connector = (AbstractConnector) endpoint.getConnector();

        if (endpoint.getSecurityFilter() != null)
        {
            try
            {
                endpoint.getSecurityFilter().authenticate(event);
            }
            catch (org.mule.api.security.SecurityException e)
            {
                // TODO MULE-863: Do we need this warning?
                logger.warn("Outbound Request was made but was not authenticated: " + e.getMessage(), e);
                connector.fireNotification(new SecurityNotification(e,
                    SecurityNotification.SECURITY_AUTHENTICATION_FAILED));
                connector.handleException(e);
                event.getMessage().setExceptionPayload(new DefaultExceptionPayload(e));
                return event;
            }
            catch (Exception e)
            {
                connector.handleException(e);
                throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
            }
        }
        return processNext(event);
    }
}
