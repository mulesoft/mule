/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.component.DefaultJavaComponent;

import java.util.Map;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * TODO
 */
public class SimpleComponentFactoryBean extends AbstractFactoryBean implements MuleContextAware
{
    private Class muleComponentClass = DefaultJavaComponent.class;

    private Class componentClass;

    private Map properties;

    private MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    @Override
    public Class getObjectType()
    {
        return muleComponentClass;
    }

    @Override
    protected Object createInstance() throws Exception
    {
        DefaultJavaComponent component = new DefaultJavaComponent();
        return null;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }

    public Class getMuleComponentClass()
    {
        return muleComponentClass;
    }

    public void setMuleComponentClass(Class muleComponentClass)
    {
        this.muleComponentClass = muleComponentClass;
    }

    public Class getComponentClass()
    {
        return componentClass;
    }

    public void setComponentClass(Class componentClass)
    {
        this.componentClass = componentClass;
    }

    public Map getProperties()
    {
        return properties;
    }

    public void setProperties(Map properties)
    {
        this.properties = properties;
    }
}
