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
import java.util.ArrayList;
import java.util.Collection;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.source.MessageSource;
import org.mule.api.transformer.Transformer;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.service.DefaultServiceExceptionStrategy;

public abstract class AbstractFlowConstructBuilder<T extends AbstractFlowConstructBuilder, F extends AbstractFlowConstruct>
{
    protected static final DefaultServiceExceptionStrategy DEFAULT_SERVICE_EXCEPTION_STRATEGY = new DefaultServiceExceptionStrategy();

    protected String name;
    protected ExceptionListener exceptionListener;
    protected String address;
    protected EndpointBuilder endpointBuilder;
    protected InboundEndpoint inboundEndpoint;
    protected Collection<? extends Transformer> inboundTransformers;
    protected Collection<? extends Transformer> inboundResponseTransformers;

    public T named(String name)
    {
        this.name = name;
        return (T) this;
    }

    public T withExceptionListener(ExceptionListener exceptionListener)
    {
        this.exceptionListener = exceptionListener;
        return (T) this;
    }

    public T receivingOn(InboundEndpoint inboundEndpoint)
    {
        this.inboundEndpoint = inboundEndpoint;
        return (T) this;
    }

    public T receivingOn(EndpointBuilder endpointBuilder)
    {
        this.endpointBuilder = endpointBuilder;
        return (T) this;
    }

    public T receivingOn(String address)
    {
        this.address = address;
        return (T) this;
    }

    public T transformingInboundRequestsWith(Collection<? extends Transformer> transformers)
    {
        this.inboundTransformers = transformers;
        return (T) this;
    }

    public T transformingInboundResponsesWith(Collection<? extends Transformer> responseTransformers)
    {
        this.inboundResponseTransformers = responseTransformers;
        return (T) this;
    }

    public F in(MuleContext muleContext) throws MuleException
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
            endpointBuilder.setTransformers(new ArrayList<Transformer>(inboundTransformers));
        }

        if (inboundResponseTransformers != null)
        {
            endpointBuilder.setResponseTransformers(new ArrayList<Transformer>(inboundResponseTransformers));
        }

        return endpointBuilder.buildInboundEndpoint();
    }

    protected abstract MessageExchangePattern getInboundMessageExchangePattern();
}
