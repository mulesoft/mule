/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.database;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.module.db.internal.domain.connection.DbPoolingProfile;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.domain.database.GenericDbConfig;
import org.mule.module.db.internal.domain.type.CompositeDbTypeManager;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.DbTypeManager;
import org.mule.module.db.internal.domain.type.JdbcTypes;
import org.mule.module.db.internal.domain.type.MetadataDbTypeManager;
import org.mule.module.db.internal.domain.type.StaticDbTypeManager;
import org.mule.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private Map<String, String> connectionProperties;
    private List<DbType> customDataTypes;
    private RetryPolicyTemplate retryPolicyTemplate;

    @Override
    public Class<?> getObjectType()
    {
        return GenericDbConfig.class;
    }

    @Override
    protected DbConfig createInstance() throws Exception
    {
        validate();

        DbTypeManager dbTypeManager = doCreateTypeManager();
        dbConfig = doCreateDbConfig(dataSource, dbTypeManager);
        dbConfig.setPoolingProfile(poolingProfile);
        dbConfig.setUseXaTransactions(useXaTransactions);
        dbConfig.setUrl(getEffectiveUrl());
        dbConfig.setConnectionTimeout(connectionTimeout);
        dbConfig.setUsername(user);
        dbConfig.setPassword(password);
        dbConfig.setTransactionIsolation(transactionIsolation);
        dbConfig.setDriverClassName(driverClassName);
        dbConfig.setMuleContext(muleContext);
        dbConfig.setRetryPolicyTemplate(retryPolicyTemplate);

        return dbConfig;
    }

    protected DbTypeManager doCreateTypeManager()
    {
        List<DbTypeManager> typeManagers = new ArrayList<DbTypeManager>();

        typeManagers.add(new MetadataDbTypeManager());

        if (customDataTypes.size() > 0)
        {
            typeManagers.add(new StaticDbTypeManager(customDataTypes));
        }
        List<DbType> vendorDataTypes = getVendorDataTypes();

        if (vendorDataTypes.size() > 0)
        {
            typeManagers.add(new StaticDbTypeManager(vendorDataTypes));
        }

        typeManagers.add(new StaticDbTypeManager(JdbcTypes.types));

        return new CompositeDbTypeManager(typeManagers);
    }

    protected List<DbType> getVendorDataTypes()
    {
        return Collections.EMPTY_LIST;
    }

    protected GenericDbConfig doCreateDbConfig(DataSource datasource, DbTypeManager dbTypeManager)
    {
        return new GenericDbConfig(datasource, name, dbTypeManager);
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

    public List<DbType> getCustomDataTypes()
    {
        return customDataTypes;
    }

    public void setCustomDataTypes(List<DbType> customDataTypes)
    {
        this.customDataTypes = customDataTypes;
    }

    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return retryPolicyTemplate;
    }

    public void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate)
    {
        this.retryPolicyTemplate = retryPolicyTemplate;
    }
}
