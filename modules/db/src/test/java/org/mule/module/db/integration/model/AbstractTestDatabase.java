/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model;

import org.mule.common.metadata.datatype.DataType;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractTestDatabase
{

    public static final Planet[] PLANET_TEST_VALUES = {Planet.VENUS, Planet.EARTH, Planet.MARS};
    public static final Alien[] ALIEN_TEST_VALUES = {Alien.MONGUITO, Alien.ET};

    private static final Log logger = LogFactory.getLog(AbstractTestDatabase.class);
    public static final String NO_SQLXML_SUPPORT_ERROR = "Database does not support SQLXML type";
    public static final String NO_RESULSET_FROM_FUNCTION_SUPPORT_ERROR = "Database does not support returning a resultset from a function";

    public void deletePlanetTable(Connection connection) throws SQLException
    {
        executeUpdate(connection, "DELETE FROM PLANET");
    }

    public abstract void createPlanetTable(Connection connection) throws SQLException;


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

    public final void populatePlanetTable(Connection connection, Planet[] testValues) throws SQLException
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

    public void createDefaultDatabaseConfig(DataSource dataSource) throws SQLException
    {
        Connection connection = dataSource.getConnection();
        try
        {
            connection.setAutoCommit(false);

            createPlanetTestTable(connection);

            if (supportsXmlType())
            {
                createAlienTestTable(connection);
            }

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

    private void createAlienTestTable(Connection connection) throws SQLException
    {
        try
        {
            deleteAlienTable(connection);
        }
        catch (Exception e)
        {
            createAlienTable(connection);
        }

        populateAlienTable(connection, ALIEN_TEST_VALUES);
    }

    private void populateAlienTable(Connection connection, Alien[] testValues) throws SQLException
    {
        QueryRunner qr = new QueryRunner();

        for (Alien alien : testValues)
        {
            int updated = qr.update(connection, getInsertAlienSql(alien));

            if (logger.isDebugEnabled())
            {
                logger.debug(updated + " rows updated");
            }
        }
    }

    protected void createAlienTable(Connection connection) throws SQLException
    {
        throw new UnsupportedOperationException(NO_SQLXML_SUPPORT_ERROR);
    }

    protected void deleteAlienTable(Connection connection) throws SQLException
    {
        executeUpdate(connection, "DELETE FROM ALIEN");
    }

    protected String getInsertAlienSql(Alien alien)
    {
        throw new UnsupportedOperationException(NO_SQLXML_SUPPORT_ERROR);
    }

    protected boolean supportsXmlType()
    {
        return false;
    }

    protected void createPlanetTestTable(Connection connection) throws SQLException
    {
        try
        {
            deletePlanetTable(connection);
        }
        catch (Exception e)
        {
            createPlanetTable(connection);
        }

        populatePlanetTable(connection, PLANET_TEST_VALUES);
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

    public void createFunctionGetRecords(DataSource dataSource) throws SQLException
    {
        throw new UnsupportedOperationException(NO_RESULSET_FROM_FUNCTION_SUPPORT_ERROR);
    }

    public abstract void createStoredProcedureUpdateTestType1(DataSource dataSource) throws SQLException;

    public abstract void createStoredProcedureParameterizedUpdateTestType1(DataSource dataSource) throws SQLException;

    public abstract void createStoredProcedureCountRecords(DataSource dataSource) throws SQLException;

    public abstract void createStoredProcedureGetSplitRecords(DataSource dataSource) throws SQLException;

    public abstract void createStoredProcedureDoubleMyInt(DataSource dataSource) throws SQLException;

    public abstract void createStoredProcedureMultiplyInts(DataSource dataSource) throws SQLException;

    public abstract void createStoredProcedureConcatenateStrings(DataSource dataSource) throws SQLException;

    public void createStoredProcedureGetAlienDescription(DataSource dataSource) throws SQLException
    {
        throw new UnsupportedOperationException(NO_SQLXML_SUPPORT_ERROR);
    }

    public void createStoredProcedureUpdateAlienDescription(DataSource dataSource) throws SQLException
    {
        throw new UnsupportedOperationException(NO_SQLXML_SUPPORT_ERROR);
    }

    public DataType getIdFieldInputMetaDataType()
    {
        return DataType.INTEGER;
    }

    public DataType getIdFieldOutputMetaDataType()
    {
        return DataType.INTEGER;
    }

    public DataType getPositionFieldOutputMetaDataType()
    {
        return DataType.INTEGER;
    }

    public Class getIdFieldJavaClass()
    {
        return Number.class;
    }

    public Class getDefaultAutoGeneratedKeyClass()
    {
        return Number.class;
    }
}