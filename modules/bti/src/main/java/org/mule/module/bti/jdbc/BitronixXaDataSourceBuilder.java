/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bti.jdbc;


import org.mule.api.MuleContext;
import org.mule.module.bti.BitronixConfigurationUtil;
import org.mule.util.Preconditions;

import javax.sql.XADataSource;

import bitronix.tm.resource.jdbc.PoolingDataSource;

public class BitronixXaDataSourceBuilder
{

    private int minPoolSize = 4;
    private int maxPoolSize = 16;
    private int maxIdleTime = 60;
    private String name;
    private XADataSource dataSource;

    public BitronixXaDataSourceWrapper build(MuleContext muleContext)
    {
        Preconditions.checkState(name != null, "name is required");
        Preconditions.checkState(dataSource != null, "dataSource is required");
        Preconditions.checkState(minPoolSize >= 0, "minPoolSize must be greater or equal than 0");
        Preconditions.checkState(maxPoolSize > 0, "maxPoolSize must be greater than 0");
        Preconditions.checkState(maxIdleTime > 0, "maxIdleTime must be greater than 0");

        PoolingDataSource poolingDataSource;
        synchronized (BitronixJdbcXaDataSourceProvider.class)
        {
            //TODO change once BTM-131 gets fixed
            BitronixJdbcXaDataSourceProvider.xaDatasourceHolder = dataSource;
            poolingDataSource = new PoolingDataSource();
            poolingDataSource.setClassName(BitronixJdbcXaDataSourceProvider.class.getCanonicalName());
            poolingDataSource.setMinPoolSize(minPoolSize);
            poolingDataSource.setMaxPoolSize(maxPoolSize);
            poolingDataSource.setMaxIdleTime(maxIdleTime);
            poolingDataSource.setAcquireIncrement(1);
            poolingDataSource.setAllowLocalTransactions(true);
            poolingDataSource.setAutomaticEnlistingEnabled(false);
            poolingDataSource.setUniqueName(BitronixConfigurationUtil.createUniqueIdForResource(muleContext, name));
            poolingDataSource.init();
        }

        return new BitronixXaDataSourceWrapper(poolingDataSource, muleContext);
    }

    public int getMinPoolSize()
    {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize)
    {
        this.minPoolSize = minPoolSize;
    }

    public int getMaxPoolSize()
    {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize)
    {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMaxIdleTime()
    {
        return maxIdleTime;
    }

    public void setMaxIdleTime(int maxIdleTime)
    {
        this.maxIdleTime = maxIdleTime;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public XADataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(XADataSource dataSource)
    {
        this.dataSource = dataSource;
    }
}
