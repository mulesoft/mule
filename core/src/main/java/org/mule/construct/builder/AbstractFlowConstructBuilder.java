/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct.builder;

import java.util.List;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.AbstractFlowConstruct;

@SuppressWarnings("unchecked")
public abstract class AbstractFlowConstructBuilder<T extends AbstractFlowConstructBuilder, F extends AbstractFlowConstruct>
{
    private MessagingExceptionHandler exceptionListener;
    private InboundEndpoint inboundEndpoint;
    private EndpointBuilder inboundEndpointBuilder;
    private String inboundAddress;

    protected String name;

    // setters should be exposed only for builders where it makes sense
    protected List<MessageProcessor> inboundTransformers;
    protected List<MessageProcessor> inboundResponseTransformers;

    public T name(String name)
    {
        this.name = name;
        return (T) this;
    }

    public T exceptionStrategy(MessagingExceptionHandler exceptionListener)
    {
        this.exceptionListener = exceptionListener;
        return (T) this;
    }

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

    public F build(MuleContext muleContext) throws MuleException
    {
        final F flowConstruct = buildFlowConstruct(muleContext);
        addExceptionListener(flowConstruct);
        return flowConstruct;
    }

    public F buildAndRegister(MuleContext muleContext) throws MuleException
    {
        final F flowConstruct = build(muleContext);
        muleContext.getRegistry().registerObject(flowConstruct.getName(), flowConstruct);
        return flowConstruct;
    }

    protected abstract F buildFlowConstruct(MuleContext muleContext) throws MuleException;

    protected void addExceptionListener(AbstractFlowConstruct flowConstruct)
    {
        if (exceptionListener != null)
        {
            flowConstruct.setExceptionListener(exceptionListener);
        }
    }

    protected InboundEndpoint getOrBuildInboundEndpoint(MuleContext muleContext) throws MuleException
    {
        if (inboundEndpoint != null)
        {
            return inboundEndpoint;
        }

        if (inboundEndpointBuilder == null)
        {
            inboundEndpointBuilder = muleContext.getRegistry().lookupEndpointFactory().getEndpointBuilder(
                inboundAddress);
        }

        inboundEndpointBuilder.setExchangePattern(getInboundMessageExchangePattern());

        if (inboundTransformers != null)
        {
            inboundEndpointBuilder.setMessageProcessors(inboundTransformers);
        }

        if (inboundResponseTransformers != null)
        {
            inboundEndpointBuilder.setResponseMessageProcessors(inboundResponseTransformers);
        }

        doConfigureInboundEndpointBuilder(muleContext, inboundEndpointBuilder);

        return inboundEndpointBuilder.buildInboundEndpoint();
    }

    protected abstract MessageExchangePattern getInboundMessageExchangePattern();

    protected void doConfigureInboundEndpointBuilder(MuleContext muleContext, EndpointBuilder endpointBuilder)
    {
        // template method
    }
}
