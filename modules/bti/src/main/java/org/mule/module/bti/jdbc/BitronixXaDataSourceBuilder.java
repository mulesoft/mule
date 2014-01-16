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

    public static final int DEFAULT_MIN_POOL_SIZE = 4;
    public static final int DEFAULT_MAX_POOL_SIZE = 16;
    public static final int DEFAULT_MAX_IDLE = 60;
    public static final int DEFAULT_ACQUISITION_TIMEOUT_SECONDS = 30;
    public static final int DEFAULT_PREPARED_STATEMENT_CACHE_SIZE = 5;
    public static final int DEFAULT_ACQUIRE_INCREMENT = 1;

    private int minPoolSize = DEFAULT_MIN_POOL_SIZE;
    private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
    private int maxIdleTime = DEFAULT_MAX_IDLE;
    private int acquisitionTimeoutSeconds = DEFAULT_ACQUISITION_TIMEOUT_SECONDS;
    private int preparedStatementCacheSize = DEFAULT_PREPARED_STATEMENT_CACHE_SIZE;
    private int acquireIncrement = DEFAULT_ACQUIRE_INCREMENT;
    private String name;
    private XADataSource dataSource;

    public BitronixXaDataSourceWrapper build(MuleContext muleContext)
    {
        Preconditions.checkState(name != null, "name is required");
        Preconditions.checkState(dataSource != null, "dataSource is required");
        Preconditions.checkState(minPoolSize >= 0, "minPoolSize must be greater or equal than 0");
        Preconditions.checkState(maxPoolSize > 0, "maxPoolSize must be greater than 0");
        Preconditions.checkState(maxIdleTime > 0, "maxIdleTime must be greater than 0");
        Preconditions.checkState(acquisitionTimeoutSeconds >= 0, "acquisitionTimeoutSeconds must equal to or grater than 0");
        Preconditions.checkState(preparedStatementCacheSize > 0, "preparedStatementCacheSize must equal to or grater than 0");
        Preconditions.checkState(acquireIncrement > 0, "acquireIncrement must be greater than 0");

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
            poolingDataSource.setAcquireIncrement(acquireIncrement);
            poolingDataSource.setAllowLocalTransactions(true);
            poolingDataSource.setAutomaticEnlistingEnabled(false);
            poolingDataSource.setUniqueName(BitronixConfigurationUtil.createUniqueIdForResource(muleContext, name));
            poolingDataSource.setAcquisitionTimeout(acquisitionTimeoutSeconds);
            poolingDataSource.setPreparedStatementCacheSize(preparedStatementCacheSize);
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

    public int getAcquisitionTimeoutSeconds()
    {
        return acquisitionTimeoutSeconds;
    }

    public void setAcquisitionTimeoutSeconds(int acquisitionTimeoutSeconds)
    {
        this.acquisitionTimeoutSeconds = acquisitionTimeoutSeconds;
    }

    public int getPreparedStatementCacheSize()
    {
        return preparedStatementCacheSize;
    }

    public void setPreparedStatementCacheSize(int preparedStatementCacheSize)
    {
        this.preparedStatementCacheSize = preparedStatementCacheSize;
    }

    public int getAcquireIncrement()
    {
        return acquireIncrement;
    }

    public void setAcquireIncrement(int acquireIncrement)
    {
        this.acquireIncrement = acquireIncrement;
    }
}
