/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.database;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.common.DefaultResult;
import org.mule.common.DefaultTestResult;
import org.mule.common.Result;
import org.mule.common.TestResult;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataKey;
import org.mule.module.db.internal.domain.connection.ConnectionFactory;
import org.mule.module.db.internal.domain.connection.DbPoolingProfile;
import org.mule.module.db.internal.domain.connection.RetryConnectionFactory;
import org.mule.module.db.internal.domain.connection.SimpleConnectionFactory;
import org.mule.module.db.internal.domain.connection.TransactionalDbConnectionFactory;
import org.mule.module.db.internal.domain.transaction.TransactionCoordinationDbTransactionManager;
import org.mule.module.db.internal.domain.type.DbTypeManager;
import org.mule.module.db.internal.domain.xa.CompositeDataSourceDecorator;
import org.mule.retry.policies.NoRetryPolicyTemplate;

import com.mchange.v2.c3p0.DataSources;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.enhydra.jdbc.standard.StandardDataSource;
import org.enhydra.jdbc.standard.StandardXADataSource;

/**
 * Defines a database configuration that is not customized for any particular
 * database vendor
 */
public class GenericDbConfig implements DbConfig, Initialisable
{

    private DataSource dataSource;
    private final String name;
    private TransactionalDbConnectionFactory dbConnectionFactory;
    private final DbTypeManager dbTypeManager;

    private final CompositeDataSourceDecorator databaseDecorator = new CompositeDataSourceDecorator();
    private DbPoolingProfile poolingProfile;
    private boolean useXaTransactions;
    private String username;
    private String password;
    private int connectionTimeout;
    private int transactionIsolation;
    private String driverClassName;
    private MuleContext muleContext;
    private String url;
    private RetryPolicyTemplate retryPolicyTemplate;

    public GenericDbConfig(DataSource dataSource, String name, DbTypeManager dbTypeManager)
    {
        this.dataSource = dataSource;
        this.name = name;
        this.dbTypeManager = dbTypeManager;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public DataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public TransactionalDbConnectionFactory getConnectionFactory()
    {
        return dbConnectionFactory;
    }

    public DbTypeManager getDbTypeManager()
    {
        return dbTypeManager;
    }

    @Override
    public TestResult test()
    {
        Connection connection = null;

        try
        {
            connection = dataSource.getConnection();
            return new DefaultTestResult(Result.Status.SUCCESS);
        }
        catch (SQLException e)
        {
            return new DefaultTestResult(Result.Status.FAILURE, e.getMessage());
        }
        finally
        {
            if (connection != null)
            {
                try
                {
                    connection.close();
                }
                catch (SQLException e)
                {
                    // Ignore
                }
            }
        }
    }

    @Override
    public Result<List<MetaDataKey>> getMetaDataKeys()
    {
        List<MetaDataKey> keys = new ArrayList<MetaDataKey>();

        return new DefaultResult<List<MetaDataKey>>(keys, Result.Status.SUCCESS, "Successfully obtained metadata");
    }

    @Override
    public Result<MetaData> getMetaData(MetaDataKey metaDataKey)
    {
        return new DefaultResult<MetaData>(null, Result.Status.SUCCESS, "No metadata obtained");
    }

    @Override
    public void initialise() throws InitialisationException
    {
        databaseDecorator.init(muleContext);
        DataSource instanceDataSource = dataSource;
        if (instanceDataSource == null)
        {
            try
            {
                dataSource = createDataSource();
            }
            catch (Exception e)
            {
                throw new InitialisationException(e, this);
            }
        }
        dataSource = decorateDataSourceIfRequired(dataSource);

        ConnectionFactory connectionFactory;
        if (retryPolicyTemplate == null)
        {
            connectionFactory = new SimpleConnectionFactory();
        }
        else
        {
            connectionFactory = new RetryConnectionFactory(retryPolicyTemplate, new SimpleConnectionFactory());
        }

        dbConnectionFactory = new TransactionalDbConnectionFactory(new TransactionCoordinationDbTransactionManager(), dbTypeManager, connectionFactory, this.getDataSource());
    }

    protected DataSource createDataSource() throws Exception
    {
        if (poolingProfile == null)
        {
            DataSource singleDataSource = createSingleDataSource();
            return singleDataSource;
        }
        else
        {
            return createPooledDataSource();
        }
    }

    private DataSource createSingleDataSource() throws SQLException
    {
        StandardDataSource dataSource = useXaTransactions ? new StandardXADataSource() : new StandardDataSource();
        dataSource.setDriverName(driverClassName);
        if (connectionTimeout >= 0)
        {
            dataSource.setLoginTimeout(connectionTimeout);
        }
        dataSource.setPassword(password);
        dataSource.setTransactionIsolation(transactionIsolation);
        dataSource.setUrl(url);
        dataSource.setUser(username);

        return dataSource;
    }

    private DataSource createPooledDataSource() throws PropertyVetoException, SQLException
    {
        if (useXaTransactions)
        {
            return createPooledXaDataSource();
        }
        else
        {
            return createPooledStandardDataSource();
        }
    }

    private DataSource createPooledStandardDataSource() throws PropertyVetoException, SQLException
    {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("maxPoolSize", poolingProfile.getMaxPoolSize());
        config.put("minPoolSize", poolingProfile.getMinPoolSize());
        config.put("initialPoolSize", poolingProfile.getMinPoolSize());
        config.put("checkoutTimeout", poolingProfile.getMaxWaitMillis());
        config.put("acquireIncrement", poolingProfile.getAcquireIncrement());
        config.put("maxStatements", 0);
        config.put("maxStatementsPerConnection", poolingProfile.getPreparedStatementCacheSize());

        return DataSources.pooledDataSource(createSingleDataSource(), config);
    }

    private DataSource createPooledXaDataSource() throws SQLException
    {
        DataSource dataSource = createSingleDataSource();
        return decorateDataSourceIfRequired(dataSource);
    }

    private DataSource decorateDataSourceIfRequired(DataSource dataSource)
    {
        return databaseDecorator.decorate(dataSource, getName(), poolingProfile, muleContext);
    }

    public void setPoolingProfile(DbPoolingProfile poolingProfile)
    {
        this.poolingProfile = poolingProfile;
    }

    public void setUseXaTransactions(boolean useXaTransactions)
    {
        this.useXaTransactions = useXaTransactions;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public void setConnectionTimeout(int connectionTimeout)
    {
        this.connectionTimeout = connectionTimeout;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public void setTransactionIsolation(int transactionIsolation)
    {
        this.transactionIsolation = transactionIsolation;
    }

    public void setDriverClassName(String driverClassName)
    {
        this.driverClassName = driverClassName;
    }

    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate)
    {
        this.retryPolicyTemplate = retryPolicyTemplate;
    }
}
