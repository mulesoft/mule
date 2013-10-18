/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.store.ObjectStore;

import java.io.Serializable;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Lookup an {@link ObjectStore} from the registry.
 */
public class ObjectStoreFromRegistryFactoryBean extends AbstractFactoryBean<ObjectStore<Serializable>> implements MuleContextAware
{
    private String objectStoreName;
    private MuleContext muleContext;

    public ObjectStoreFromRegistryFactoryBean(String name)
    {
        super();
        objectStoreName = name;
    }
    
    @Override
    public Class<?> getObjectType()
    {
        return ObjectStore.class;
    }

    @Override
    protected ObjectStore<Serializable> createInstance() throws Exception
    {
        return muleContext.getRegistry().lookupObject(objectStoreName);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }
}
