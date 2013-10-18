/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct.builder;

import java.util.Arrays;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.component.JavaComponent;
import org.mule.api.lifecycle.Callable;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.object.ObjectFactory;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.component.DefaultJavaComponent;
import org.mule.component.SimpleCallableJavaComponent;
import org.mule.construct.SimpleService;
import org.mule.construct.SimpleService.Type;
import org.mule.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.object.PrototypeObjectFactory;
import org.mule.object.SingletonObjectFactory;

/**
 * Fluent API for the creation of a SimpleService.
 */
public class SimpleServiceBuilder extends
    AbstractFlowConstructWithSingleInboundEndpointBuilder<SimpleServiceBuilder, SimpleService>
{
    protected Type type = SimpleService.Type.DIRECT;
    protected Component component;

    @Override
    protected MessageExchangePattern getInboundMessageExchangePattern()
    {
        return MessageExchangePattern.REQUEST_RESPONSE;
    }

    public SimpleServiceBuilder transformers(Transformer... transformers)
    {
        this.transformers = Arrays.asList((MessageProcessor[]) transformers);
        return this;
    }

    public SimpleServiceBuilder responseTransformers(Transformer... responseTransformers)
    {
        this.responseTransformers = Arrays.asList((MessageProcessor[]) responseTransformers);
        return this;
    }

    public SimpleServiceBuilder component(Class<?> componentClass)
    {
        return component(new PrototypeObjectFactory(componentClass));
    }

    public SimpleServiceBuilder component(ObjectFactory objectFactory)
    {
        return component(new DefaultJavaComponent(objectFactory));
    }

    public SimpleServiceBuilder component(Callable callable)
    {
        return component(new SimpleCallableJavaComponent(callable));
    }

    public SimpleServiceBuilder component(Object o)
    {
        return component(new SingletonObjectFactory(o));
    }

    public SimpleServiceBuilder type(Type type)
    {
        this.type = type;
        return this;
    }

    public SimpleServiceBuilder component(Component component)
    {
        if (component instanceof JavaComponent)
        {
            final JavaComponent javaComponent = (JavaComponent) component;

            if (javaComponent.getEntryPointResolverSet() == null)
            {
                javaComponent.setEntryPointResolverSet(createEntryPointResolverSet());
            }
        }

        this.component = component;
        return this;
    }

    @Override
    protected SimpleService buildFlowConstruct(MuleContext muleContext) throws MuleException
    {
        return new SimpleService(name, muleContext, getOrBuildInboundEndpoint(muleContext), transformers,
            responseTransformers, component, type);
    }

    protected EntryPointResolverSet createEntryPointResolverSet()
    {
        return new LegacyEntryPointResolverSet();
    }
}
