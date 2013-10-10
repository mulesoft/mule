/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityFilter;
import org.mule.config.ExceptionHelper;
import org.mule.context.notification.SecurityNotification;
import org.mule.endpoint.EndpointAware;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transport.AbstractConnector;

/**
 * Filters the flow using the specified {@link SecurityFilter}. 
 * If unauthorised the flow is stopped and therefore the
 * message is not send or dispatched by the transport. When unauthorised the request
 * message is returned as the response.
 */
public class SecurityFilterMessageProcessor extends AbstractInterceptingMessageProcessor implements EndpointAware
{
    private SecurityFilter filter;

    /**
     * For IoC only
     * @deprecated Use SecurityFilterMessageProcessor(SecurityFilter filter) instead
     */
    public SecurityFilterMessageProcessor()
    {
        super();
    }

    public SecurityFilterMessageProcessor(SecurityFilter filter)
    {
        this.filter = filter;
    }

    public SecurityFilter getFilter()
    {
        return filter;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (filter != null)
        {
            try
            {
                filter.doFilter(event);
            }
            catch (SecurityException e)
            {
                e = (SecurityException) ExceptionHelper.sanitizeIfNeeded(e);
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

    public void setFilter(SecurityFilter filter)
    {
        this.filter = filter;
    }

    public void setEndpoint(ImmutableEndpoint ep)
    {
        if (filter instanceof EndpointAware)
        {
            ((EndpointAware) filter).setEndpoint(ep);
        }
    }
}
