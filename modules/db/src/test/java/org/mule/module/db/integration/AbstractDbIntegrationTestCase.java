/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.module.db.integration.DbTestUtil.selectData;
import static org.mule.module.db.integration.TestRecordUtil.assertRecords;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.Field;
import org.mule.module.db.integration.model.Record;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;
import org.mule.tck.junit4.FunctionalTestCase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public abstract class AbstractDbIntegrationTestCase extends FunctionalTestCase
{

    private final String dataSourceConfigResource;
    protected final AbstractTestDatabase testDatabase;

    public AbstractDbIntegrationTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        this.dataSourceConfigResource = dataSourceConfigResource;
        this.testDatabase = testDatabase;
    }

    @Before
    public void configDB() throws SQLException
    {
        testDatabase.createDefaultDatabaseConfig(getDefaultDataSource());
    }

    protected DataSource getDefaultDataSource()
    {
        DbConfigResolver dbConfigResolver = muleContext.getRegistry().get("dbConfig");
        DbConfig config = resolveConfig(dbConfigResolver);

        return config.getDataSource();
    }

    protected DbConfig resolveConfig(DbConfigResolver dbConfigResolver)
    {
        return dbConfigResolver.resolve(null);
    }

    @Override
    protected final String[] getConfigFiles()
    {
        StringBuilder builder = new StringBuilder();

        builder.append(getDatasourceConfigurationResource());

        for (String resource : getFlowConfigurationResources())
        {
            if (builder.length() != 0)
            {
                builder.append(",");
            }

            builder.append(resource);
        }

        return builder.toString().split(",");
    }

    protected final String getDatasourceConfigurationResource()
    {
        return dataSourceConfigResource;
    }

    protected abstract String[] getFlowConfigurationResources();

    protected void assertPlanetRecordsFromQuery(String... names) throws SQLException
    {
        if (names.length == 0)
        {
            throw new IllegalArgumentException("Must provide at least a name to query on the DB");
        }

        StringBuilder conditionBuilder = new StringBuilder();
        List<Record> records = new ArrayList<Record>(names.length);

        for (String name : names)
        {
            if (conditionBuilder.length() != 0)
            {
                conditionBuilder.append(",");
            }
            conditionBuilder.append("'").append(name).append("'");
            records.add(new Record(new Field("NAME", name)));
        }

        List<Map<String, String>> result = selectData(String.format("select * from PLANET where name in (%s)", conditionBuilder.toString()), getDefaultDataSource());

        assertRecords(result, records.toArray(new Record[0]));
    }

    protected void assertDeletedPlanetRecords(String... names) throws SQLException
    {
        if (names.length == 0)
        {
            throw new IllegalArgumentException("Must provide at least a name to query on the DB");
        }

        StringBuilder conditionBuilder = new StringBuilder();

        for (String name : names)
        {
            if (conditionBuilder.length() != 0)
            {
                conditionBuilder.append(",");
            }
            conditionBuilder.append("'").append(name).append("'");
        }

        List<Map<String, String>> result = selectData(String.format("select * from PLANET where name in (%s)", conditionBuilder.toString()), getDefaultDataSource());
        assertThat(result.size(), equalTo(0));
    }

}
