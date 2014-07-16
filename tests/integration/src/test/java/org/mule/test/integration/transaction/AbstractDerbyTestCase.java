/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.util.MuleDerbyTestDatabase;
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
    
    private static MuleDerbyTestDatabase muleDerbyTestDatabase = new MuleDerbyTestDatabase("derby.properties");

    public AbstractDerbyTestCase(AbstractServiceAndFlowTestCase.ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @BeforeClass
    public static void startDatabase() throws Exception
    {
        muleDerbyTestDatabase.startDatabase();
    }

    @AfterClass
    public static void stopDatabase() throws SQLException
    {
        muleDerbyTestDatabase.stopDatabase();
    }

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        muleDerbyTestDatabase.emptyTestTable();
    }

    protected Connection getConnection() throws Exception
    {
        return muleDerbyTestDatabase.getConnection();
    }

    protected List execSqlQuery(String sql) throws Exception
    {
        return muleDerbyTestDatabase.execSqlQuery(sql);
    }

    protected int execSqlUpdate(String sql) throws Exception
    {
        return muleDerbyTestDatabase.execSqlUpdate(sql);
    }
    
}


