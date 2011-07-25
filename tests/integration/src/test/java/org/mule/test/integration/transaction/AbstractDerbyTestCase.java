/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transaction;

import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.util.MuleDerbyTestUtils;
import org.mule.transport.jdbc.JdbcUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class AbstractDerbyTestCase extends AbstractServiceAndFlowTestCase
{
    
    private static String connectionString;

    public AbstractDerbyTestCase(AbstractServiceAndFlowTestCase.ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @BeforeClass
    public static void startDatabase() throws Exception
    {
        String dbName = MuleDerbyTestUtils.loadDatabaseName("derby.properties", "database.name");

        MuleDerbyTestUtils.defaultDerbyCleanAndInit("derby.properties", "database.name");
        connectionString = "jdbc:derby:" + dbName;
    }

    @AfterClass
    public static void stopDatabase() throws SQLException
    {
        MuleDerbyTestUtils.stopDatabase();
    }

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        emptyTable();
    }

    /**
     * Subclasses must implement this method to either delete the table if it doesn't yet
     * exist or delete all records from it.
     */
    protected abstract void emptyTable() throws Exception;

    protected Connection getConnection() throws Exception
    {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        return DriverManager.getConnection(connectionString);
    }

    protected List execSqlQuery(String sql) throws Exception
    {
        Connection con = null;
        try
        {
            con = getConnection();
            return (List)new QueryRunner().query(con, sql, new ArrayListHandler());
        }
        finally
        {
            JdbcUtils.close(con);
        }
    }

    protected int execSqlUpdate(String sql) throws Exception
    {
        Connection con = null;
        try
        {
            con = getConnection();
            return new QueryRunner().update(con, sql);
        }
        finally
        {
            JdbcUtils.close(con);
        }
    }
    
}


