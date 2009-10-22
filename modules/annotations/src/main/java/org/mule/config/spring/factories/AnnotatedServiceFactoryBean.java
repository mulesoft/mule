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

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.model.Model;
import org.mule.api.service.Service;
import org.mule.impl.annotations.AnnotatedServiceBuilder;
import org.mule.impl.model.resolvers.AnnotatedEntryPointResolver;
import org.mule.model.resolvers.DefaultEntryPointResolverSet;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * A factory bean used to create a {@link org.mule.api.service.Service} object from an annotated service.
 */
public class AnnotatedServiceFactoryBean extends AbstractFactoryBean implements MuleContextAware
{
    private Model model;
    private String name;
    private MuleContext muleContext;
    private Service service;

    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    public Model getModel()
    {
        return model;
    }

    public void setModel(Model model)
    {
        this.model = model;
        //TODO:  we should just be able to add the resolver to the top, but its not possible right now
        DefaultEntryPointResolverSet resolverSet = new DefaultEntryPointResolverSet();
        resolverSet.getEntryPointResolvers().clear();
        resolverSet.addEntryPointResolver(new AnnotatedEntryPointResolver());
        this.model.setEntryPointResolverSet(resolverSet);
    }


    protected Object createInstance() throws Exception
    {
        AnnotatedServiceBuilder builder = new AnnotatedServiceBuilder(muleContext);
        builder.setModel(getModel());
        //TODO fix this or delete it
        service = builder.createService(null);
        return service;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public Class getObjectType()
    {
        return Service.class;
    }

    //@java.lang.Override
    public void afterPropertiesSet() throws Exception
    {
        super.afterPropertiesSet();
        service.setMuleContext(muleContext);
        service.initialise();
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
