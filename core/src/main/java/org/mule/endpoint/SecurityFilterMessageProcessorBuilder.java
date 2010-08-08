/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.processor.SecurityFilterMessageProcessor;

/**
 * A holder for security filters.  This will create the correct inbound or outbound
 * SecurityFilterMessageProcessor when an endpoint is constructed.
 */
public class SecurityFilterMessageProcessorBuilder implements EndpointAwareMessageProcessor

{
    private EndpointSecurityFilter securityFilter;

    public SecurityFilterMessageProcessorBuilder()
    {
    }

    public SecurityFilterMessageProcessorBuilder(EndpointSecurityFilter securityFilter)
    {
        this.securityFilter = securityFilter;
    }

    public EndpointSecurityFilter getSecurityFilter()
    {
        return securityFilter;
    }

    public void setSecurityFilter(EndpointSecurityFilter securityFilter)
    {
        this.securityFilter = securityFilter;
    }

    /**
     * Should never be called
     */
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * create the proper sort of message processor, injecting the endpoint into it
     */
    public MessageProcessor injectEndpoint(ImmutableEndpoint endpoint)
    {
        if (securityFilter != null)
        {
            securityFilter.setEndpoint(endpoint);
        }
        return new SecurityFilterMessageProcessor(securityFilter);
    }
}

