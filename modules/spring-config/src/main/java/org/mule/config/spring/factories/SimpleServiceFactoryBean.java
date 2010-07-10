/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.factories;

import java.util.List;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.object.ObjectFactory;
import org.mule.api.transformer.Transformer;
import org.mule.component.DefaultJavaComponent;
import org.mule.config.spring.util.SpringBeanLookup;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.construct.SimpleService;
import org.mule.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.object.PrototypeObjectFactory;

public class SimpleServiceFactoryBean extends AbstractFlowConstructFactoryBean
{
    private EndpointBuilder endpointBuilder;
    private String address;
    private List<Transformer> transformers;
    private List<Transformer> responseTransformers;
    private String componentClass;
    private String componentBeanName;

    public Class<?> getObjectType()
    {
        return SimpleService.class;
    }

    @Override
    protected AbstractFlowConstruct createFlowConstruct() throws MuleException
    {
        return new SimpleService(muleContext, name, buildInboundEndpoint(), buildComponent());
    }

    public void setEndpoint(EndpointBuilder endpointBuilder)
    {
        this.endpointBuilder = endpointBuilder;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public void setTransformers(List<Transformer> transformers)
    {
        this.transformers = transformers;
    }

    public void setResponseTransformers(List<Transformer> responseTransformers)
    {
        this.responseTransformers = responseTransformers;
    }

    public void setComponentClass(String componentClass)
    {
        this.componentClass = componentClass;
    }

    public void setComponentBeanName(String componentBeanName)
    {
        this.componentBeanName = componentBeanName;
    }

    private InboundEndpoint buildInboundEndpoint() throws MuleException
    {
        if (endpointBuilder == null)
        {
            endpointBuilder = muleContext.getRegistry().lookupEndpointFactory().getEndpointBuilder(address);
        }

        endpointBuilder.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);
        endpointBuilder.setTransformers(transformers);
        endpointBuilder.setResponseTransformers(responseTransformers);
        return endpointBuilder.buildInboundEndpoint();
    }

    private Component buildComponent()
    {
        ObjectFactory objectFactory = getComponentObjectFactory();
        DefaultJavaComponent component = new DefaultJavaComponent(objectFactory);
        component.setEntryPointResolverSet(new LegacyEntryPointResolverSet());
        return component;
    }

    private ObjectFactory getComponentObjectFactory()
    {
        if (componentBeanName != null)
        {
            SpringBeanLookup sbl = new SpringBeanLookup();
            sbl.setApplicationContext(applicationContext);
            sbl.setBean(componentBeanName);
            return sbl;
        }

        return new PrototypeObjectFactory(componentClass);
    }
}
