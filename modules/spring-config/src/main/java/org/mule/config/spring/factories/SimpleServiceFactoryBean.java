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

import java.beans.ExceptionListener;
import java.util.List;

import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.transformer.Transformer;
import org.mule.config.spring.util.SpringBeanLookup;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.construct.SimpleService;
import org.mule.construct.builder.SimpleServiceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * Builds SimpleService instances by using the SimpleServiceBuilder.
 */
public class SimpleServiceFactoryBean extends AbstractFlowConstructFactoryBean
{
    private final SimpleServiceBuilder newSimpleService = new SimpleServiceBuilder();

    private SpringBeanLookup springBeanLookup;

    public Class<?> getObjectType()
    {
        return SimpleService.class;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        super.setApplicationContext(applicationContext);

        if (springBeanLookup != null)
        {
            springBeanLookup.setApplicationContext(applicationContext);
        }
    }

    @Override
    protected AbstractFlowConstruct createFlowConstruct() throws MuleException
    {
        return newSimpleService.build(muleContext);
    }

    public void setName(String name)
    {
        newSimpleService.name(name);
    }

    public void setExceptionListener(ExceptionListener exceptionListener)
    {
        newSimpleService.exceptionStrategy(exceptionListener);
    }

    public void setEndpoint(EndpointBuilder endpointBuilder)
    {
        newSimpleService.inboundEndpoint(endpointBuilder);
    }

    public void setAddress(String address)
    {
        newSimpleService.inboundAddress(address);
    }

    public void setTransformers(List<? extends Transformer> transformers)
    {
        newSimpleService.inboundTransformers(transformers.toArray(new Transformer[0]));
    }

    public void setResponseTransformers(List<? extends Transformer> responseTransformers)
    {
        newSimpleService.inboundResponseTransformers(responseTransformers.toArray(new Transformer[0]));
    }

    public void setComponentClass(Class<?> componentClass)
    {
        newSimpleService.component(componentClass);
    }

    public void setComponentBeanName(String componentBeanName)
    {
        springBeanLookup = new SpringBeanLookup();
        springBeanLookup.setBean(componentBeanName);
        newSimpleService.component(springBeanLookup);
    }

    public void setComponent(Component component)
    {
        newSimpleService.component(component);
    }

}
