/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.config.domain.database;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.db.domain.database.DbConfig;
import org.mule.module.db.domain.database.GenericDbConfig;
import org.mule.module.db.domain.connection.DbPoolingProfile;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.AbstractFactoryBean;

public class DbConfigFactoryBean extends AbstractFactoryBean<DbConfig> implements MuleContextAware, Initialisable
{

    private String name;
    private String url;
    private String driverClassName;
    private int connectionTimeout;
    private String password;
    private String user;
    private int transactionIsolation = -1;
    private DataSource dataSource;
    private MuleContext muleContext;
    private boolean useXaTransactions;
    private DbPoolingProfile poolingProfile;
    private GenericDbConfig dbConfig;
    private Map<String, String> properties;

    @Override
    public Class<?> getObjectType()
    {
        return GenericDbConfig.class;
    }

    @Override
    protected DbConfig createInstance() throws Exception
    {
        dbConfig = doCreateDbConfig(dataSource);
        dbConfig.setPoolingProfile(poolingProfile);
        dbConfig.setUseXaTransactions(useXaTransactions);
        dbConfig.setUrl(getEffectiveUrl());
        dbConfig.setConnectionTimeout(connectionTimeout);
        dbConfig.setUsername(user);
        dbConfig.setPassword(password);
        dbConfig.setTransactionIsolation(transactionIsolation);
        dbConfig.setDriverClassName(driverClassName);
        dbConfig.setMuleContext(muleContext);

        return dbConfig;
    }

    protected GenericDbConfig doCreateDbConfig(DataSource datasource)
    {
        return new GenericDbConfig(datasource, name);
    }

    protected String getEffectiveUrl()
    {
        return url;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
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
        this.transactionIsolation = transactionIsolation;
    }

    public void setconnectionTimeout(int connectionTimeout)
    {
        this.connectionTimeout = connectionTimeout;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public void setDriverClassName(String driverClassName)
    {
        this.driverClassName = driverClassName;
    }

    public String getDriverClassName()
    {
        return driverClassName;
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }

    public void setProperties(Map<String, String> properties)
    {
        this.properties = properties;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public void setUseXaTransactions(boolean useXaTransactions)
    {
        this.useXaTransactions = useXaTransactions;
    }

    public void setPoolingProfile(DbPoolingProfile poolingProfile)
    {
        this.poolingProfile = poolingProfile;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        dbConfig.initialise();
    }
}
