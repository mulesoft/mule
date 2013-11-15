/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bti.config;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.bti.jms.BitronixConnectionFactoryPoolBuilder;
import org.mule.module.bti.jms.BitronixConnectionFactoryWrapper;
import org.mule.util.Preconditions;

import javax.jms.XAConnectionFactory;

import org.springframework.beans.factory.config.AbstractFactoryBean;


public class ConnectionFactoryPoolFactoryBean extends AbstractFactoryBean<BitronixConnectionFactoryWrapper> implements MuleContextAware, Initialisable
{

    private BitronixConnectionFactoryPoolBuilder builder = new BitronixConnectionFactoryPoolBuilder();
    private MuleContext muleContext;
    private BitronixConnectionFactoryWrapper instance;

    @Override
    public Class<?> getObjectType()
    {
        return BitronixConnectionFactoryWrapper.class;
    }

    @Override
    protected BitronixConnectionFactoryWrapper createInstance() throws Exception
    {
        Preconditions.checkState(instance == null, "Only one instance can be created");
        instance = builder.build(muleContext);
        return instance;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (instance != null)
        {
            instance.initialise();
        }
    }

    public int getMinPoolSize()
    {
        return builder.getMinPoolSize();
    }

    public void setMinPoolSize(int minPoolSize)
    {
        builder.setMinPoolSize(minPoolSize);
    }

    public int getMaxPoolSize()
    {
        return builder.getMaxPoolSize();
    }

    public void setMaxPoolSize(int maxPoolSize)
    {
        builder.setMaxPoolSize(maxPoolSize);
    }

    public int getMaxIdleTime()
    {
        return builder.getMaxIdleTime();
    }

    public void setMaxIdleTime(int maxIdleTime)
    {
        builder.setMaxIdleTime(maxIdleTime);
    }

    public XAConnectionFactory getConnectionFactory()
    {
        return builder.getConnectionFactory();
    }

    public void setConnectionFactory(XAConnectionFactory connectionFactory)
    {
        builder.setConnectionFactory(connectionFactory);
    }

    public String getName()
    {
        return builder.getName();
    }

    public void setName(String name)
    {
        builder.setName(name);
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }
}
