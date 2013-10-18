/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct.builder;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.construct.AbstractFlowConstruct;

@SuppressWarnings("unchecked")
public abstract class AbstractFlowConstructWithSingleInboundEndpointBuilder<T extends AbstractFlowConstructBuilder<?, ?>, F extends AbstractFlowConstruct>
    extends AbstractFlowConstructBuilder<T, F>
{
    private InboundEndpoint inboundEndpoint;
    private EndpointBuilder inboundEndpointBuilder;
    private String inboundAddress;

    public T inboundEndpoint(InboundEndpoint inboundEndpoint)
    {
        this.inboundEndpoint = inboundEndpoint;
        return (T) this;
    }

    public T inboundEndpoint(EndpointBuilder inboundEndpointBuilder)
    {
        this.inboundEndpointBuilder = inboundEndpointBuilder;
        return (T) this;
    }

    public T inboundAddress(String inboundAddress)
    {
        this.inboundAddress = inboundAddress;
        return (T) this;
    }

    protected InboundEndpoint getOrBuildInboundEndpoint(MuleContext muleContext) throws MuleException
    {
        if (inboundEndpoint != null)
        {
            return inboundEndpoint;
        }

        if (inboundEndpointBuilder == null)
        {
            inboundEndpointBuilder = muleContext.getEndpointFactory().getEndpointBuilder(inboundAddress);
        }

        inboundEndpointBuilder.setExchangePattern(getInboundMessageExchangePattern());

        doConfigureInboundEndpointBuilder(muleContext, inboundEndpointBuilder);

        return inboundEndpointBuilder.buildInboundEndpoint();
    }

    protected abstract MessageExchangePattern getInboundMessageExchangePattern();

    protected void doConfigureInboundEndpointBuilder(MuleContext muleContext, EndpointBuilder endpointBuilder)
    {
        // template method
    }
}
