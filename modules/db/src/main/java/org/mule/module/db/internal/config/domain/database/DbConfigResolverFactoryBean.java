/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.database;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.config.spring.factories.AnnotatedObjectFactoryBean;
import org.mule.module.db.internal.domain.connection.DbPoolingProfile;
import org.mule.module.db.internal.domain.database.ConfigurableDbConfigFactory;
import org.mule.module.db.internal.domain.database.DataSourceConfig;
import org.mule.module.db.internal.domain.database.DataSourceFactory;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.domain.database.GenericDbConfigFactory;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;
import org.mule.module.db.internal.resolver.database.DynamicDbConfigResolver;
import org.mule.module.db.internal.resolver.database.StaticDbConfigResolver;
import org.mule.util.Preconditions;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

/**
 * Creates {@link DbConfigResolver} instances
 */
public class DbConfigResolverFactoryBean extends AnnotatedObjectFactoryBean<DbConfigResolver> implements AnnotatedObject, MuleContextAware, Disposable
{

    private MuleContext muleContext;
    private String name;
    private DataSource dataSource;
    private Map<String, String> connectionProperties;
    private DataSourceFactory dataSourceFactory;
    private final DataSourceConfig dataSourceConfig = new DataSourceConfig();
    private final ConfigurableDbConfigFactory dbConfigFactory;

    @SuppressWarnings("unused")
    public DbConfigResolverFactoryBean()
    {
        // Default constructor needed by Spring
        this(new GenericDbConfigFactory());
    }

    public DbConfigResolverFactoryBean(ConfigurableDbConfigFactory dbConfigFactory)
    {
        this.dbConfigFactory = dbConfigFactory;
    }

    @Override
    public Class<?> getObjectType()
    {
        return DbConfigResolver.class;
    }

    @Override
    protected DbConfigResolver doCreateInstance() throws Exception
    {
        validate();

        // Updates URL based on connection properties
        dataSourceConfig.setUrl(getEffectiveUrl());

        dataSourceFactory = createDataSourceFactory();

        if (dataSourceConfig.isDynamic())
        {
            DataSourceFactory dataSourceFactory = new DataSourceFactory(name);
            dataSourceFactory.setMuleContext(muleContext);

            return new DynamicDbConfigResolver(name, dbConfigFactory, dataSourceFactory, dataSourceConfig);
        }
        else
        {
            DataSource instanceDataSource;
            if (dataSource == null)
            {
                instanceDataSource = dataSourceFactory.create(dataSourceConfig);
            }
            else
            {
                instanceDataSource = dataSourceFactory.decorateDataSource(dataSource, dataSourceConfig.getPoolingProfile(), muleContext);
            }

            DbConfig dbConfig = dbConfigFactory.create(name, getAnnotations(), instanceDataSource);
            dbConfig.setAnnotations(getAnnotations());

            return new StaticDbConfigResolver(dbConfig);
        }
    }

    protected DataSourceFactory createDataSourceFactory()
    {
        DataSourceFactory dataSourceFactory = new DataSourceFactory(name);
        dataSourceFactory.setMuleContext(muleContext);

        return dataSourceFactory;
    }

    protected void validate()
    {
        if (dataSource != null)
        {
            Preconditions.checkState(connectionProperties.isEmpty(), "connection-properties cannot be specified when a DataSource was provided");
        }
    }

    protected String getEffectiveUrl()
    {
        return dataSourceConfig.getUrl();
    }

    public String getUrl()
    {
        return dataSourceConfig.getUrl();
    }

    public void setUrl(String url)
    {
        dataSourceConfig.setUrl(url);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setTransactionIsolation(int transactionIsolation)
    {
        dataSourceConfig.setTransactionIsolation(transactionIsolation);
    }

    public void setconnectionTimeout(int connectionTimeout)
    {
        dataSourceConfig.setConnectionTimeout(connectionTimeout);
    }

    public void setPassword(String password)
    {
        dataSourceConfig.setPassword(password);
    }

    public void setUser(String user)
    {
        dataSourceConfig.setUser(user);
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public void setDriverClassName(String driverClassName)
    {
        dataSourceConfig.setDriverClassName(driverClassName);
    }

    public Map<String, String> getConnectionProperties()
    {
        return connectionProperties;
    }

    public void setConnectionProperties(Map<String, String> connectionProperties)
    {
        this.connectionProperties = connectionProperties;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        dataSourceConfig.setMuleContext(muleContext);
        ((GenericDbConfigFactory) dbConfigFactory).setMuleContext(muleContext);
    }

    public void setUseXaTransactions(boolean useXaTransactions)
    {
        dataSourceConfig.setUseXaTransactions(useXaTransactions);
    }

    public void setPoolingProfile(DbPoolingProfile poolingProfile)
    {
        dataSourceConfig.setPoolingProfile(poolingProfile);
    }

    public void setCustomDataTypes(List<DbType> customDataTypes)
    {
        this.dbConfigFactory.setCustomDataTypes(customDataTypes);
    }

    public void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate)
    {
        this.dbConfigFactory.setRetryPolicyTemplate(retryPolicyTemplate);
    }

    @Override
    public void dispose()
    {
        if (dataSourceFactory != null)
        {
            dataSourceFactory.dispose();
        }
    }

}
