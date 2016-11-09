/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.database;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.module.db.internal.domain.connection.DbPoolingProfile;
import org.mule.module.db.internal.domain.xa.CompositeDataSourceDecorator;
import org.mule.util.concurrent.ConcurrentHashSet;

import com.mchange.v2.c3p0.DataSources;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.enhydra.jdbc.standard.StandardDataSource;
import org.enhydra.jdbc.standard.StandardXADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link DataSource} instances
 */
public class DataSourceFactory implements MuleContextAware, Disposable
{

    private static final Logger logger = LoggerFactory.getLogger(DataSourceFactory.class);

    private final String name;
    private final Set<DataSource> pooledDataSources = new ConcurrentHashSet();
    private final Set<Disposable> disposableDataSources = new ConcurrentHashSet();
    private MuleContext muleContext;

    public DataSourceFactory(String name)
    {
        this.name = name;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    /**
     * Creates a dataSource from a given dataSource config
     *
     * @param dataSourceConfig describes how to create the dataSource
     * @return a non null dataSource
     * @throws SQLException in case there is a problem creating the dataSource
     */
    public DataSource create(DataSourceConfig dataSourceConfig) throws SQLException
    {
        DataSource dataSource;

        if (dataSourceConfig.getPoolingProfile() == null)
        {
            dataSource = createSingleDataSource(dataSourceConfig);
        }
        else
        {
            dataSource = createPooledDataSource(dataSourceConfig);
        }

        if (dataSourceConfig.isUseXaTransactions())
        {
            dataSource = decorateDataSource(dataSource, dataSourceConfig.getPoolingProfile(), getMuleContext());
        }

        if (!(dataSourceConfig.getPoolingProfile() == null || dataSourceConfig.isUseXaTransactions()))
        {
            pooledDataSources.add(dataSource);
        }
        else if (dataSource instanceof Disposable)
        {
            disposableDataSources.add((Disposable) dataSource);
        }

        return dataSource;
    }

    public DataSource decorateDataSource(DataSource dataSource, DbPoolingProfile poolingProfile, MuleContext muleContext)
    {
        CompositeDataSourceDecorator dataSourceDecorator = new CompositeDataSourceDecorator();
        dataSourceDecorator.init(muleContext);

        return dataSourceDecorator.decorate(dataSource, name, poolingProfile, muleContext);
    }

    protected DataSource createSingleDataSource(DataSourceConfig resolvedDataSourceConfig) throws SQLException
    {
        StandardDataSource dataSource = resolvedDataSourceConfig.isUseXaTransactions() ? new StandardXADataSource() : new StandardDataSource();
        dataSource.setDriverName(resolvedDataSourceConfig.getDriverClassName());
        if (resolvedDataSourceConfig.getConnectionTimeout() >= 0)
        {
            dataSource.setLoginTimeout(resolvedDataSourceConfig.getConnectionTimeout());
        }
        dataSource.setPassword(resolvedDataSourceConfig.getPassword());
        dataSource.setTransactionIsolation(resolvedDataSourceConfig.getTransactionIsolation());
        dataSource.setUrl(resolvedDataSourceConfig.getUrl());
        dataSource.setUser(resolvedDataSourceConfig.getUser());

        return dataSource;
    }

    protected DataSource createPooledDataSource(DataSourceConfig dataSourceConfig) throws SQLException
    {
        if (dataSourceConfig.isUseXaTransactions())
        {
            return createSingleDataSource(dataSourceConfig);
        }
        else
        {
            return createPooledStandardDataSource(createSingleDataSource(dataSourceConfig), dataSourceConfig.getPoolingProfile());
        }
    }

    protected DataSource createPooledStandardDataSource(DataSource dataSource, DbPoolingProfile poolingProfile) throws SQLException
    {
        Map<String, Object> config = new HashMap<>();
        config.put("maxPoolSize", poolingProfile.getMaxPoolSize());
        config.put("minPoolSize", poolingProfile.getMinPoolSize());
        config.put("initialPoolSize", poolingProfile.getMinPoolSize());
        config.put("checkoutTimeout", poolingProfile.getMaxWaitMillis());
        config.put("acquireIncrement", poolingProfile.getAcquireIncrement());
        config.put("maxStatements", 0);
        config.put("testConnectionOnCheckout", "true");
        config.put("maxStatementsPerConnection", poolingProfile.getPreparedStatementCacheSize());

        return DataSources.pooledDataSource(dataSource, config);
    }

    @Override
    public void dispose()
    {
        for (DataSource pooledDataSource : pooledDataSources)
        {
            try
            {
                DataSources.destroy(pooledDataSource);
            }
            catch (SQLException e)
            {
                logger.warn("Unable to properly release pooled data source", e);
            }
        }

        for (Disposable disposableDataSource : disposableDataSources)
        {
            try
            {
                disposableDataSource.dispose();
            }
            catch (Exception e)
            {
                logger.warn("Unable to properly dispose data source", e);
            }
        }

    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }
}
