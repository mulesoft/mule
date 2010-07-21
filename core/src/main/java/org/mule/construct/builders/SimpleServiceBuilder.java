/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct.builders;

import java.beans.ExceptionListener;
import java.util.List;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.component.JavaComponent;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.object.ObjectFactory;
import org.mule.api.transformer.Transformer;
import org.mule.component.DefaultJavaComponent;
import org.mule.construct.SimpleService;
import org.mule.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.object.PrototypeObjectFactory;

/**
 * Fluent API for the creation of a SimpleService.
 */
public class SimpleServiceBuilder extends AbstractFlowConstructBuilder
{
    // TODO (DDO) unit test
    protected static final LegacyEntryPointResolverSet DEFAULT_ENTRY_POINT_RESOLVER_SET = new LegacyEntryPointResolverSet();

    protected String address;
    protected EndpointBuilder endpointBuilder;
    protected List<Transformer> transformers;
    protected List<Transformer> responseTransformers;
    protected Component component;

    public SimpleServiceBuilder named(String name)
    {
        this.name = name;
        return this;
    }

    public SimpleServiceBuilder withExceptionListener(ExceptionListener exceptionListener)
    {
        this.exceptionListener = exceptionListener;
        return this;
    }

    public SimpleServiceBuilder receivingOn(EndpointBuilder endpointBuilder)
    {
        this.endpointBuilder = endpointBuilder;
        return this;
    }

    public SimpleServiceBuilder receivingOn(String address)
    {
        this.address = address;
        return this;
    }

    public SimpleServiceBuilder transformingRequestsWith(List<Transformer> transformers)
    {
        this.transformers = transformers;
        return this;
    }

    public SimpleServiceBuilder transformingResponseWith(List<Transformer> responseTransformers)
    {
        this.responseTransformers = responseTransformers;
        return this;
    }

    public SimpleServiceBuilder serving(String componentClass)
    {
        return serving(new PrototypeObjectFactory(componentClass));
    }

    public SimpleServiceBuilder serving(ObjectFactory objectFactory)
    {
        return serving(new DefaultJavaComponent(objectFactory));
    }

    public SimpleServiceBuilder serving(Component component)
    {
        // TODO (DDO) support REST/WS annotated components

        if (component instanceof JavaComponent)
        {
            JavaComponent javaComponent = (JavaComponent) component;

            if (javaComponent.getEntryPointResolverSet() == null)
            {
                javaComponent.setEntryPointResolverSet(DEFAULT_ENTRY_POINT_RESOLVER_SET);
            }
        }

        this.component = component;
        return this;
    }

    public SimpleService in(MuleContext muleContext) throws MuleException
    {
        SimpleService simpleService = new SimpleService(muleContext, name, buildInboundEndpoint(muleContext),
            component);

        addExceptionListener(simpleService);

        return simpleService;
    }

    private InboundEndpoint buildInboundEndpoint(MuleContext muleContext) throws MuleException
    {
        if (endpointBuilder == null)
        {
            endpointBuilder = muleContext.getRegistry().lookupEndpointFactory().getEndpointBuilder(address);
        }

        // forced to request-response for SimpleService
        endpointBuilder.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);
        endpointBuilder.setTransformers(transformers);
        endpointBuilder.setResponseTransformers(responseTransformers);
        return endpointBuilder.buildInboundEndpoint();
    }

}
