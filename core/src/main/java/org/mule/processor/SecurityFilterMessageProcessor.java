/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.NonBlockingSupported;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.security.SecurityFilter;
import org.mule.endpoint.EndpointAware;

/**
 * Filters the flow using the specified {@link SecurityFilter}. 
 * If unauthorised the flow is stopped and therefore the
 * message is not send or dispatched by the transport. When unauthorised the request
 * message is returned as the response.
 */
public class SecurityFilterMessageProcessor extends AbstractInterceptingMessageProcessor implements EndpointAware, Initialisable, NonBlockingSupported
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

    @Override
    public void initialise() throws InitialisationException
    {
        LifecycleUtils.initialiseIfNeeded(filter, muleContext);
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
