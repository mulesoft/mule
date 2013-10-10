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
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.construct.AbstractFlowConstruct;

@SuppressWarnings("unchecked")
public abstract class AbstractFlowConstructWithSingleInboundAndOutboundEndpointBuilder<T extends AbstractFlowConstructBuilder<?, ?>, F extends AbstractFlowConstruct>
    extends AbstractFlowConstructWithSingleInboundEndpointBuilder<T, F>
{
    private OutboundEndpoint outboundEndpoint;
    private EndpointBuilder outboundEndpointBuilder;
    private String outboundAddress;

    public T outboundEndpoint(OutboundEndpoint outboundEndpoint)
    {
        this.outboundEndpoint = outboundEndpoint;
        return (T) this;
    }

    public T outboundEndpoint(EndpointBuilder outboundEndpointBuilder)
    {
        this.outboundEndpointBuilder = outboundEndpointBuilder;
        return (T) this;
    }

    public T outboundAddress(String outboundAddress)
    {
        this.outboundAddress = outboundAddress;
        return (T) this;
    }

    protected OutboundEndpoint getOrBuildOutboundEndpoint(MuleContext muleContext) throws MuleException
    {
        if (outboundEndpoint != null)
        {
            return outboundEndpoint;
        }

        if (outboundEndpointBuilder == null)
        {
            outboundEndpointBuilder = muleContext.getEndpointFactory().getEndpointBuilder(outboundAddress);
        }

        outboundEndpointBuilder.setExchangePattern(getOutboundMessageExchangePattern());

        doConfigureOutboundEndpointBuilder(muleContext, outboundEndpointBuilder);

        return outboundEndpointBuilder.buildOutboundEndpoint();
    }

    protected abstract MessageExchangePattern getOutboundMessageExchangePattern();

    protected void doConfigureOutboundEndpointBuilder(MuleContext muleContext, EndpointBuilder endpointBuilder)
    {
        // template method
    }

}
