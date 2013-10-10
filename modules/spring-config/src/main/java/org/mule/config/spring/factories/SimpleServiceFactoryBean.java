/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.factories;

import org.mule.api.component.Component;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.config.spring.util.SpringBeanLookup;
import org.mule.construct.SimpleService;
import org.mule.construct.SimpleService.Type;
import org.mule.construct.builder.AbstractFlowConstructBuilder;
import org.mule.construct.builder.SimpleServiceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * Builds SimpleService instances by using the SimpleServiceBuilder.
 */
public class SimpleServiceFactoryBean extends AbstractFlowConstructFactoryBean
{
    final SimpleServiceBuilder simpleServiceBuilder = new SimpleServiceBuilder();

    private SpringBeanLookup springBeanLookup;

    public Class<?> getObjectType()
    {
        return SimpleService.class;
    }

    @Override
    protected AbstractFlowConstructBuilder<SimpleServiceBuilder, SimpleService> getFlowConstructBuilder()
    {
        return simpleServiceBuilder;
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

    public void setEndpointBuilder(EndpointBuilder endpointBuilder)
    {
        simpleServiceBuilder.inboundEndpoint(endpointBuilder);
    }

    public void setAddress(String address)
    {
        simpleServiceBuilder.inboundAddress(address);
    }

    public void setTransformers(Transformer... transformers)
    {
        simpleServiceBuilder.transformers(transformers);
    }

    public void setResponseTransformers(Transformer... responseTransformers)
    {
        simpleServiceBuilder.responseTransformers(responseTransformers);
    }

    public void setComponentClass(Class<?> componentClass)
    {
        simpleServiceBuilder.component(componentClass);
    }

    public void setComponentBeanName(String componentBeanName)
    {
        springBeanLookup = new SpringBeanLookup();
        springBeanLookup.setBean(componentBeanName);
        simpleServiceBuilder.component(springBeanLookup);
    }
    
    public void setType(Type type) {
        simpleServiceBuilder.type(type);
    }

    public void setMessageProcessor(MessageProcessor component)
    {
        if (component instanceof Component)
        {
            simpleServiceBuilder.component((Component) component);
        }
        else
        {
            throw new IllegalArgumentException("Single Component message processor is required");
        }
    }
}
