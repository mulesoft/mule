/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
