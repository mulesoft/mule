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

import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.config.spring.util.SpringBeanLookup;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.construct.SimpleService;
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

    @Override
    protected AbstractFlowConstruct createFlowConstruct() throws MuleException
    {
        return simpleServiceBuilder.build(muleContext);
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

    public void setComponent(Component component)
    {
        simpleServiceBuilder.component(component);
    }

}
