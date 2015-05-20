/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.database;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mule.module.db.internal.domain.connection.DbPoolingProfile;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.mchange.v2.c3p0.PooledDataSource;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;

@SmallTest
public class DataSourceFactoryTestCase extends AbstractMuleTestCase
{

    private final PooledDataSource dataSource = mock(PooledDataSource.class);
    private final DataSourceFactory dataSourceFactory = new DataSourceFactory("test")
    {
        @Override
        protected DataSource createPooledDataSource(DataSourceConfig dataSourceConfig) throws SQLException
        {
            return dataSource;
        }

        @Override
        protected DataSource decorateDataSource(DataSource dataSource, DbPoolingProfile poolingProfile)
        {
            return dataSource;
        }
    };

    @Test
    public void destroysPooledDataSource() throws Exception
    {

        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        dataSourceConfig.setPoolingProfile(mock(DbPoolingProfile.class));

        doDisposeTest(dataSourceConfig, 1);
    }

    @Test
    public void doesNotDestroySingleDataSource() throws Exception
    {
        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        doDisposeTest(dataSourceConfig, 0);
    }

    @Test
    public void doesNotDestroySingleXaDataSource() throws Exception
    {
        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        dataSourceConfig.setUseXaTransactions(true);
        doDisposeTest(dataSourceConfig, 0);
    }

    @Test
    public void doesNotDestroyPooledXaDataSource() throws Exception
    {
        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        dataSourceConfig.setUseXaTransactions(true);
        dataSourceConfig.setPoolingProfile(mock(DbPoolingProfile.class));
        doDisposeTest(dataSourceConfig, 0);
    }

    private void doDisposeTest(DataSourceConfig dataSourceConfig, int expectedDisposeInvocations) throws SQLException
    {
        dataSourceFactory.create(dataSourceConfig);

        dataSourceFactory.dispose();

        verify(dataSource, times(expectedDisposeInvocations)).close(false);
    }
}