/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.security.SecurityFilter;
import org.mule.endpoint.EndpointAware;

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
            filter.doFilter(event);
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
