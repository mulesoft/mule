/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.executeddl;

import static org.junit.Assert.assertEquals;
import static org.mule.module.db.integration.DbTestUtil.selectData;
import static org.mule.module.db.integration.TestRecordUtil.assertRecords;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.junit.Before;

public abstract class AbstractExecuteDdlTestCase extends AbstractDbIntegrationTestCase
{

    public AbstractExecuteDdlTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Before
    public void deleteTestDdlTable() throws Exception
    {

        Connection connection = null;
        try
        {
            DataSource dataSource = getDefaultDataSource();
            connection = dataSource.getConnection();

            QueryRunner qr = new QueryRunner(dataSource);
            qr.update(connection, "DROP TABLE TestDdl");
        }
        catch (SQLException e)
        {
            // Ignore: table does not exist
        }
        finally
        {
            if (connection != null)
            {
                connection.close();
            }
        }
    }

    protected void assertTableCreation(Object payload) throws SQLException
    {
        assertEquals(0, payload);

        List<Map<String, String>> result = selectData("select * from TestDdl", getDefaultDataSource());
        assertRecords(result);
    }
}
