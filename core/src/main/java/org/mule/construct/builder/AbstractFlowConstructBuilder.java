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

import java.beans.ExceptionListener;
import java.util.List;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.api.transformer.Transformer;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.service.DefaultServiceExceptionStrategy;

import edu.emory.mathcs.backport.java.util.Arrays;

public abstract class AbstractFlowConstructBuilder<T extends AbstractFlowConstructBuilder, F extends AbstractFlowConstruct>
{
    protected static final DefaultServiceExceptionStrategy DEFAULT_SERVICE_EXCEPTION_STRATEGY = new DefaultServiceExceptionStrategy();

    protected String name;
    protected ExceptionListener exceptionListener;
    protected String address;
    protected EndpointBuilder endpointBuilder;
    protected InboundEndpoint inboundEndpoint;
    protected List<MessageProcessor> inboundTransformers;
    protected List<MessageProcessor> inboundResponseTransformers;

    public T name(String name)
    {
        this.name = name;
        return (T) this;
    }

    public T exceptionStrategy(ExceptionListener exceptionListener)
    {
        this.exceptionListener = exceptionListener;
        return (T) this;
    }

    public T inboundEndpoint(InboundEndpoint inboundEndpoint)
    {
        this.inboundEndpoint = inboundEndpoint;
        return (T) this;
    }

    public T inboundEndpoint(EndpointBuilder endpointBuilder)
    {
        this.endpointBuilder = endpointBuilder;
        return (T) this;
    }

    public T inboundAddress(String address)
    {
        this.address = address;
        return (T) this;
    }

    public T inboundTransformers(Transformer... transformers)
    {
        this.inboundTransformers = Arrays.asList(transformers);
        return (T) this;
    }

    public T inboundResponseTransformers(Transformer... responseTransformers)
    {
        this.inboundResponseTransformers = Arrays.asList(responseTransformers);
        return (T) this;
    }

    public F build(MuleContext muleContext) throws MuleException
    {
        F flowConstruct = buildFlowConstruct(muleContext);
        addExceptionListener(flowConstruct);
        return flowConstruct;
    }

    protected abstract F buildFlowConstruct(MuleContext muleContext) throws MuleException;

    protected void addExceptionListener(AbstractFlowConstruct flowConstruct)
    {
        if (exceptionListener != null)
        {
            flowConstruct.setExceptionListener(exceptionListener);
        }
        else
        {
            flowConstruct.setExceptionListener(DEFAULT_SERVICE_EXCEPTION_STRATEGY);
        }
    }

    protected MessageSource buildMessageSource(MuleContext muleContext) throws MuleException
    {
        if (inboundEndpoint != null)
        {
            return inboundEndpoint;
        }

        if (endpointBuilder == null)
        {
            endpointBuilder = muleContext.getRegistry().lookupEndpointFactory().getEndpointBuilder(address);
        }

        endpointBuilder.setExchangePattern(getInboundMessageExchangePattern());

        if (inboundTransformers != null)
        {
            endpointBuilder.setMessageProcessors(inboundTransformers);
        }

        if (inboundResponseTransformers != null)
        {
            endpointBuilder.setResponseMessageProcessors(inboundResponseTransformers);
        }

        return endpointBuilder.buildInboundEndpoint();
    }

    protected abstract MessageExchangePattern getInboundMessageExchangePattern();
}
