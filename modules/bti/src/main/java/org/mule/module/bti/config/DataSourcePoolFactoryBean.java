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
import org.mule.module.bti.jdbc.BitronixXaDataSourceBuilder;
import org.mule.module.bti.jdbc.BitronixXaDataSourceWrapper;
import org.mule.util.Preconditions;

import javax.sql.XADataSource;

import org.springframework.beans.factory.config.AbstractFactoryBean;


public class DataSourcePoolFactoryBean extends AbstractFactoryBean<BitronixXaDataSourceWrapper> implements MuleContextAware, Initialisable
{

    private BitronixXaDataSourceBuilder builder = new BitronixXaDataSourceBuilder();
    private MuleContext muleContext;
    private BitronixXaDataSourceWrapper instance;

    @Override
    public Class<?> getObjectType()
    {
        return BitronixXaDataSourceWrapper.class;
    }

    @Override
    protected BitronixXaDataSourceWrapper createInstance() throws Exception
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

    public XADataSource getDataSource()
    {
        return builder.getDataSource();
    }

    public void setDataSource(XADataSource dataSource)
    {
        builder.setDataSource(dataSource);
    }

    public String getName()
    {
        return builder.getName();
    }

    public void setName(String name)
    {
        builder.setName(name);
    }

    public int getAcquireTimeoutSeconds()
    {
        return builder.getAcquisitionTimeoutSeconds();
    }

    public void setAcquireTimeoutSeconds(int acquisitionTimeoutSeconds)
    {
        builder.setAcquisitionTimeoutSeconds(acquisitionTimeoutSeconds);
    }


    public int getPreparedStatementCacheSize()
    {
        return builder.getPreparedStatementCacheSize();
    }

    public void setPreparedStatementCacheSize(int preparedStatementCacheSize)
    {
        builder.setPreparedStatementCacheSize(preparedStatementCacheSize);
    }

    public int getAcquireIncrement()
    {
        return builder.getAcquireIncrement();
    }

    public void setAcquireIncrement(int acquireIncrement)
    {
        builder.setAcquireIncrement(acquireIncrement);
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }
}
