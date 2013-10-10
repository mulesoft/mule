/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
