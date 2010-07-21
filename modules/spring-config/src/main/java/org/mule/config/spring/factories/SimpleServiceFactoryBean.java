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
import org.mule.api.object.ObjectFactory;
import org.mule.api.transformer.Transformer;
import org.mule.component.DefaultJavaComponent;
import org.mule.config.spring.util.SpringBeanLookup;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.construct.SimpleService;
import org.mule.construct.builders.ConstructBuilders;
import org.mule.construct.builders.SimpleServiceBuilder;
import org.mule.object.PrototypeObjectFactory;

public class SimpleServiceFactoryBean extends AbstractFlowConstructFactoryBean
{
    private String componentClass;
    private String componentBeanName;
    private Component component;

    private final SimpleServiceBuilder newSimpleService = ConstructBuilders.buildSimpleService();

    public Class<?> getObjectType()
    {
        return SimpleService.class;
    }

    @Override
    protected AbstractFlowConstruct createFlowConstruct() throws MuleException
    {
        return newSimpleService.serving(getOrBuildComponent()).in(muleContext);
    }

    public void setName(String name)
    {
        newSimpleService.named(name);
    }

    public void setExceptionListener(ExceptionListener exceptionListener)
    {
        newSimpleService.withExceptionListener(exceptionListener);
    }

    public void setEndpoint(EndpointBuilder endpointBuilder)
    {
        newSimpleService.receivingOn(endpointBuilder);
    }

    public void setAddress(String address)
    {
        newSimpleService.receivingOn(address);
    }

    public void setTransformers(List<Transformer> transformers)
    {
        newSimpleService.transformingRequestsWith(transformers);
    }

    public void setResponseTransformers(List<Transformer> responseTransformers)
    {
        newSimpleService.transformingResponseWith(responseTransformers);
    }

    public void setComponentClass(String componentClass)
    {
        this.componentClass = componentClass;
    }

    public void setComponentBeanName(String componentBeanName)
    {
        this.componentBeanName = componentBeanName;
    }

    public void setComponent(Component component)
    {
        this.component = component;
    }

    private Component getOrBuildComponent()
    {
        if (component == null)
        {
            ObjectFactory objectFactory = getComponentObjectFactory();
            component = new DefaultJavaComponent(objectFactory);
        }

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
