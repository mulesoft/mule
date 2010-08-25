/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.security.SecurityException;
import org.mule.context.notification.SecurityNotification;
import org.mule.endpoint.EndpointAwareMessageProcessor;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transport.AbstractConnector;

/**
 * Filters the flow using the {@link EndpointSecurityFilter} configured on
 * the endpoint. If unauthorised the flow is stopped and therefore the
 * message is not send or dispatched by the transport. When unauthorised the request
 * message is returned as the response.
 */
public class SecurityFilterMessageProcessor extends AbstractInterceptingMessageProcessor implements EndpointAwareMessageProcessor
{
    private EndpointSecurityFilter filter;

    /**
     * For IoC only
     * @deprecated Use SecurityFilterMessageProcessor(EndpointSecurityFilter filter) instead
     */
    public SecurityFilterMessageProcessor()
    {
        super();
    }

    public SecurityFilterMessageProcessor(EndpointSecurityFilter filter)
    {
        this.filter = filter;
    }

    public EndpointSecurityFilter getFilter()
    {
        return filter;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (filter != null)
        {
            try
            {
                filter.authenticate(event);
            }
            catch (SecurityException e)
            {
                logger.warn("Outbound Request was made but was not authenticated: " + e.getMessage(), e);
                
                AbstractConnector connector = (AbstractConnector) event.getEndpoint().getConnector();
                connector.fireNotification(new SecurityNotification(e,
                    SecurityNotification.SECURITY_AUTHENTICATION_FAILED));
                
                event.getFlowConstruct().getExceptionListener().handleException(e, event);
                
                event.getMessage().setPayload(e.getLocalizedMessage());
                event.getMessage().setExceptionPayload(new DefaultExceptionPayload(e));
                return event;
            }
        }
        return processNext(event);
    }

    public void setFilter(EndpointSecurityFilter filter)
    {
        this.filter = filter;
    }

    public MessageProcessor injectEndpoint(ImmutableEndpoint ep)
    {
        filter.setEndpoint(ep);
        return this;
    }
}
