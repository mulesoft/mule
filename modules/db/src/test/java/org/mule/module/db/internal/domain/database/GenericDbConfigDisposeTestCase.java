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
import org.mule.api.lifecycle.Disposable;
import org.mule.module.db.internal.domain.connection.DbPoolingProfile;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.mchange.v2.c3p0.PooledDataSource;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;

@SmallTest
public class GenericDbConfigDisposeTestCase extends AbstractMuleTestCase
{

    private final PooledDataSource dataSource = mock(PooledDataSource.class);
    private final DisposableDataSource disposableDataSource = mock(DisposableDataSource.class);
    private GenericDbConfig dbConfig;

    public interface DisposableDataSource extends DataSource, Disposable
    {
    }

    @Test
    public void destroysPooledDataSource() throws Exception
    {
        dbConfig = new GenericDbConfig(dataSource, "test", null);
        dbConfig.setPoolingProfile(mock(DbPoolingProfile.class));

        doDataSourceDisposeTest(1);
    }

    @Test
    public void doesNotDestroySingleDataSource() throws Exception
    {
        dbConfig = new GenericDbConfig(dataSource, "test", null);
        doDataSourceDisposeTest(0);
    }

    @Test
    public void doesNotDestroySingleXaDataSource() throws Exception
    {
        dbConfig = new GenericDbConfig(dataSource, "test", null);
        dbConfig.setUseXaTransactions(true);

        doDataSourceDisposeTest(0);
    }

    @Test
    public void doesNotDestroyPooledXaDataSource() throws Exception
    {
        dbConfig = new GenericDbConfig(dataSource, "test", null);
        dbConfig.setPoolingProfile(mock(DbPoolingProfile.class));
        dbConfig.setUseXaTransactions(true);

        doDataSourceDisposeTest(0);
    }

    @Test
    public void doesNotDisposesPooledDataSource() throws Exception
    {
        dbConfig = new GenericDbConfig(disposableDataSource, "test", null);
        dbConfig.setPoolingProfile(mock(DbPoolingProfile.class));
        doDisposeTest(0);
    }

    @Test
    public void doesNotDisposesSingleDataSource() throws Exception
    {
        dbConfig = new GenericDbConfig(dataSource, "test", null);
        doDisposeTest(0);
    }

    @Test
    public void disposesSingleXaDataSource() throws Exception
    {
        dbConfig = new GenericDbConfig(disposableDataSource, "test", null);
        dbConfig.setUseXaTransactions(true);
        doDisposeTest(1);
    }

    @Test
    public void disposesPooledXaDataSource() throws Exception
    {
        dbConfig = new GenericDbConfig(disposableDataSource, "test", null);
        dbConfig.setUseXaTransactions(true);
        dbConfig.setPoolingProfile(mock(DbPoolingProfile.class));
        doDisposeTest(1);
    }

    @Test
    public void disposesDisposableDatasource() throws Exception
    {
        dbConfig = new GenericDbConfig(disposableDataSource, "test", null);
        dbConfig.setUseXaTransactions(true);

        doDisposeTest(1);
    }

    private void doDataSourceDisposeTest(int expectedInvocationCount) throws SQLException
    {
        dbConfig.dispose();

        verify(dataSource, times(expectedInvocationCount)).close(false);
    }

    private void doDisposeTest(int expectedInvocationCount) throws SQLException
    {
        dbConfig.dispose();

        verify(disposableDataSource, times(expectedInvocationCount)).dispose();
    }
}