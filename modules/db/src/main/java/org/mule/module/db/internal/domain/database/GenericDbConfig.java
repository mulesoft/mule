/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.database;

import org.mule.common.DefaultResult;
import org.mule.common.DefaultTestResult;
import org.mule.common.Result;
import org.mule.common.TestResult;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataKey;
import org.mule.module.db.internal.domain.connection.DbConnectionFactory;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.domain.type.DbTypeManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

/**
 * Defines a database configuration that is not customized for any particular
 * database vendor
 */
public class GenericDbConfig implements DbConfig
{

    private final DataSource dataSource;
    private final String name;
    private final DbConnectionFactory dbConnectionFactory;
    private final DbTypeManager dbTypeManager;

    public GenericDbConfig(DataSource dataSource, String name, DbTypeManager dbTypeManager, DbConnectionFactory dbConnectionFactory)
    {
        this.name = name;
        this.dataSource = dataSource;
        this.dbTypeManager = dbTypeManager;
        this.dbConnectionFactory = dbConnectionFactory;
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
    public DbConnectionFactory getConnectionFactory()
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
            connection = dbConnectionFactory.createConnection(TransactionalAction.NOT_SUPPORTED);

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

        return new DefaultResult<>(keys, Result.Status.SUCCESS, "Successfully obtained metadata");
    }

    @Override
    public Result<MetaData> getMetaData(MetaDataKey metaDataKey)
    {
        return new DefaultResult<>(null, Result.Status.SUCCESS, "No metadata obtained");
    }
}
