/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model;

import org.mule.common.metadata.datatype.DataType;
import org.mule.module.db.integration.TestDatabase;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractTestDatabase implements TestDatabase
{

    public static final Planet[] DEFAULT_TEST_VALUES = {Planet.VENUS, Planet.EARTH, Planet.MARS};

    private static final Log logger = LogFactory.getLog(AbstractTestDatabase.class);
    private DataType idFieldDataType;

    public void deleteDefaultTestTable(Connection connection) throws SQLException
    {
        executeUpdate(connection, "DELETE FROM PLANET");
    }

    public abstract void createDefaultTestTable(Connection connection) throws SQLException;


    public static void executeDdl(DataSource dataSource, String ddl) throws SQLException
    {
        Connection connection = dataSource.getConnection();

        try
        {
            executeDdl(connection, ddl);

        }
        finally
        {
            connection.close();
        }
    }

    public static void executeDdl(Connection connection, String ddl) throws SQLException
    {
        QueryRunner qr = new QueryRunner();
        qr.update(connection, ddl);
    }

    public void executeUpdate(Connection connection, String updateSql) throws SQLException
    {
        QueryRunner qr = new QueryRunner();
        int updated = qr.update(connection, updateSql);

        if (logger.isDebugEnabled())
        {
            logger.debug(updated + " rows updated");
        }
    }

    public final void populateDefaultTestTable(Connection connection, Planet[] testValues) throws SQLException
    {
        QueryRunner qr = new QueryRunner();

        for (Planet planet : testValues)
        {
            int updated = qr.update(connection, getInsertPlanetSql(planet.getName(), planet.getPosition()));

            if (logger.isDebugEnabled())
            {
                logger.debug(updated + " rows updated");
            }
        }
    }

    protected abstract String getInsertPlanetSql(String name, int position);

    @Override
    public void createDefaultDatabaseConfig(DataSource dataSource) throws SQLException
    {
        Connection connection = dataSource.getConnection();
        try
        {
            connection.setAutoCommit(false);

            try
            {
                deleteDefaultTestTable(connection);
            }
            catch (Exception e)
            {
                createDefaultTestTable(connection);
            }

            populateDefaultTestTable(connection, DEFAULT_TEST_VALUES);

            connection.commit();
        }
        finally
        {
            if (connection != null)
            {
                connection.close();
            }
        }
    }

    public void createStoredProcedure(DataSource dataSource, String sql) throws SQLException
    {
        Connection connection = dataSource.getConnection();

        try
        {
            Statement statement = connection.createStatement();

            statement.execute(sql);
        }
        finally
        {
            if (connection != null)
            {
                connection.close();
            }
        }
    }

    public abstract void createStoredProcedureGetRecords(DataSource dataSource) throws SQLException;

    public abstract void createStoredProcedureUpdateTestType1(DataSource dataSource) throws SQLException;

    public abstract void createStoredProcedureParameterizedUpdateTestType1(DataSource dataSource) throws SQLException;

    public abstract void createStoredProcedureCountRecords(DataSource dataSource) throws SQLException;

    public abstract void createStoredProcedureGetSplitRecords(DataSource dataSource) throws SQLException;

    public abstract void createStoredProcedureDoubleMyInt(DataSource dataSource) throws SQLException;

    public abstract void createStoredProcedureMultiplyInts(DataSource dataSource) throws SQLException;

    public abstract void createStoredProcedureConcatenateStrings(DataSource dataSource) throws SQLException;

    public DataType getIdFieldDataType()
    {
        return DataType.INTEGER;
    }
}