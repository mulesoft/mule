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

import org.junit.Test;

@SmallTest
public class GenericDbConfigDisposeTestCase extends AbstractMuleTestCase
{

    private final PooledDataSource dataSource = mock(PooledDataSource.class);
    private final GenericDbConfig dbConfig = new GenericDbConfig(dataSource, "test", null);

    @Test
    public void destroysPooledDataSource() throws Exception
    {
        dbConfig.setPoolingProfile(mock(DbPoolingProfile.class));

        doDataSourceDisposeTest(1);
    }

    @Test
    public void doesNotDestroySingleDataSource() throws Exception
    {
        doDataSourceDisposeTest(0);
    }

    @Test
    public void doesNotDestroySingleXaDataSource() throws Exception
    {
        dbConfig.setUseXaTransactions(true);

        doDataSourceDisposeTest(0);
    }

    @Test
    public void doesNotDestroyPooledXaDataSource() throws Exception
    {
        dbConfig.setPoolingProfile(mock(DbPoolingProfile.class));
        dbConfig.setUseXaTransactions(true);

        doDataSourceDisposeTest(0);
    }

    private void doDataSourceDisposeTest(int expectedInvocationCount) throws SQLException
    {
        dbConfig.dispose();

        verify(dataSource, times(expectedInvocationCount)).close(false);
    }
}