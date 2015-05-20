/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.database;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleEvent;
import org.mule.module.db.internal.domain.database.DataSourceConfig;
import org.mule.module.db.internal.domain.database.DataSourceFactory;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.domain.database.DbConfigFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;

public class DynamicDbConfigResolverTestCase extends AbstractMuleTestCase
{

    @Test
    public void resolvesDbConfig() throws Exception
    {
        DbConfig expectedDbConfig = mock(DbConfig.class);
        MuleEvent muleEvent = mock(MuleEvent.class);

        DynamicDbConfigResolver dbConfigResolver = createDbConfigFactory(expectedDbConfig);

        DbConfig dbConfig = dbConfigResolver.resolve(muleEvent);

        assertThat(dbConfig, sameInstance(expectedDbConfig));
    }

    @Test
    public void cachesResolvedDbConfig() throws Exception
    {
        DbConfig expectedDbConfig = mock(DbConfig.class);
        MuleEvent muleEvent = mock(MuleEvent.class);

        DynamicDbConfigResolver dbConfigResolver = createDbConfigFactory(expectedDbConfig);

        DbConfig dbConfig1 = dbConfigResolver.resolve(muleEvent);
        DbConfig dbConfig2 = dbConfigResolver.resolve(muleEvent);

        assertThat(dbConfig1, sameInstance(expectedDbConfig));
        assertThat(dbConfig1, sameInstance(dbConfig2));
    }

    private DynamicDbConfigResolver createDbConfigFactory(DbConfig expectedDbConfig) throws SQLException
    {
        String name = "test-dynamic-1";
        DataSourceConfig dataSourceConfig = createTestDataSourceConfig();

        DataSource dataSource = mock(DataSource.class);
        DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);
        when(dataSourceFactory.create(dataSourceConfig)).thenReturn(dataSource);

        DbConfigFactory dbConfigFactory = mock(DbConfigFactory.class);
        when(dbConfigFactory.create(name, dataSource)).thenReturn(expectedDbConfig);
        return new DynamicDbConfigResolver("test", dbConfigFactory, dataSourceFactory, dataSourceConfig);
    }

    private DataSourceConfig createTestDataSourceConfig()
    {
        DataSourceConfig dataSourceConfig = new DataSourceConfig()
        {
            @Override
            public DataSourceConfig resolve(MuleEvent muleEvent)
            {
                // Just returns the same instance
                return this;
            }
        };

        dataSourceConfig.setUrl("url");
        dataSourceConfig.setDriverClassName("driver");

        return dataSourceConfig;
    }
}