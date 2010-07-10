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

import org.mule.MessageExchangePattern;
import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
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
        return endpointBuilder.buildInboundEndpoint();
    }

    private Component buildComponent()
    {
        if (componentBeanName != null)
        {
            SpringBeanLookup sbl = new SpringBeanLookup();
            sbl.setApplicationContext(applicationContext);
            sbl.setBean(componentBeanName);
            DefaultJavaComponent component = new DefaultJavaComponent(sbl);
            // TODO fetch default one from registry?
            component.setEntryPointResolverSet(new LegacyEntryPointResolverSet());
            return component;
        }

        DefaultJavaComponent component = new DefaultJavaComponent(new PrototypeObjectFactory(componentClass));
        // TODO fetch default one from registry?
        component.setEntryPointResolverSet(new LegacyEntryPointResolverSet());
        return component;
    }
}
