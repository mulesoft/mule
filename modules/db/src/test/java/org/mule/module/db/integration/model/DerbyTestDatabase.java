/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model;

import org.mule.module.db.integration.model.derbyutil.DerbyTestStoredProcedure;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public class DerbyTestDatabase extends AbstractTestDatabase
{

    public static final String DERBY_ERROR_OBJECT_ALREADY_EXISTS = "X0Y68";

    public static String SQL_CREATE_SP_UPDATE_TEST_TYPE_1 =
            "CREATE PROCEDURE updateTestType1()\n" +
            "PARAMETER STYLE JAVA\n" +
            "LANGUAGE JAVA\n" +
            "MODIFIES SQL DATA\n" +
            "DYNAMIC RESULT SETS 0\n" +
            "EXTERNAL NAME '" + DerbyTestStoredProcedure.class.getName() + ".updateTestType1'";

    public static String SQL_CREATE_SP_PARAM_UPDATE_TEST_TYPE_1 =
            "CREATE PROCEDURE updateParamTestType1(name VARCHAR(40))\n" +
            "PARAMETER STYLE JAVA\n" +
            "LANGUAGE JAVA\n" +
            "MODIFIES SQL DATA\n" +
            "DYNAMIC RESULT SETS 0\n" +
            "EXTERNAL NAME '" + DerbyTestStoredProcedure.class.getName() + ".updateParameterizedTestType1'";

    public static String SQL_CREATE_SP_PARAM_UPDATE_PLANET_DESCRIPTION =
            "CREATE PROCEDURE updatePlanetDescription(name VARCHAR(40), description CLOB)\n" +
            "PARAMETER STYLE JAVA\n" +
            "LANGUAGE JAVA\n" +
            "MODIFIES SQL DATA\n" +
            "DYNAMIC RESULT SETS 0\n" +
            "EXTERNAL NAME '" + DerbyTestStoredProcedure.class.getName() + ".updatePlanetDescription'";

    public static String SQL_CREATE_SP_COUNT_RECORDS =
            "CREATE PROCEDURE countTestRecords(OUT COUNT INTEGER)\n" +
            "PARAMETER STYLE JAVA\n" +
            "LANGUAGE JAVA\n" +
            "READS SQL DATA\n" +
            "DYNAMIC RESULT SETS 0\n" +
            "EXTERNAL NAME '" + DerbyTestStoredProcedure.class.getName() + ".countTestRecords'";

    public static String SQL_CREATE_SP_GET_RECORDS =
            "CREATE PROCEDURE getTestRecords()\n" +
            "PARAMETER STYLE JAVA\n" +
            "LANGUAGE JAVA\n" +
            "READS SQL DATA\n" +
            "DYNAMIC RESULT SETS 1\n" +
            "EXTERNAL NAME '" + DerbyTestStoredProcedure.class.getName() + ".getTestRecords'";

    public static String SQL_CREATE_SP_GET_SPLIT_RECORDS =
            "CREATE PROCEDURE getSplitTestRecords()\n" +
            "PARAMETER STYLE JAVA\n" +
            "LANGUAGE JAVA\n" +
            "READS SQL DATA\n" +
            "DYNAMIC RESULT SETS 2\n" +
            "EXTERNAL NAME '" + DerbyTestStoredProcedure.class.getName() + ".getSplitTestRecords'";

    public static String SQL_CREATE_SP_DOUBLE_MY_INT =
            "CREATE PROCEDURE doubleMyInt(INOUT MYINT INTEGER)\n" +
            "PARAMETER STYLE JAVA\n" +
            "LANGUAGE JAVA\n" +
            "DYNAMIC RESULT SETS 0\n" +
            "EXTERNAL NAME '" + DerbyTestStoredProcedure.class.getName() + ".doubleMyInt'";

    public static String SQL_CREATE_SP_MULTIPLY_INTS =
            "CREATE PROCEDURE multiplyInts(IN INT1 INTEGER, IN INT2 INTEGER, OUT RESULT1 INTEGER, IN INT3 INTEGER, OUT RESULT2 INTEGER)\n" +
            "PARAMETER STYLE JAVA\n" +
            "LANGUAGE JAVA\n" +
            "DYNAMIC RESULT SETS 0\n" +
            "EXTERNAL NAME '" + DerbyTestStoredProcedure.class.getName() + ".multiplyInts'";

    public static String SQL_CREATE_SP_CONCATENATE_STRINGS =
            "CREATE PROCEDURE concatenateStrings(IN INT1 VARCHAR(100), IN INT2 VARCHAR(100), OUT RESULT VARCHAR(200))\n" +
            "PARAMETER STYLE JAVA\n" +
            "LANGUAGE JAVA\n" +
            "DYNAMIC RESULT SETS 0\n" +
            "EXTERNAL NAME '" + DerbyTestStoredProcedure.class.getName() + ".concatenateStrings'";

    public static final String SQL_CREATE_DELAY_FUNCTION =
            "create function delay(SECONDS INTEGER) " +
            "returns INTEGER language java parameter style java external name " +
            "'" + DerbyTestStoredProcedure.class.getName() + ".timeDelay'";

    public static final String SQL_CREATE_CONTACT_DETAIL_FUNCTION =
            "create function createContactDetails(DESCRIPTION VARCHAR(32), PHONE VARCHAR(32), EMAIL VARCHAR(32)) " +
            "returns CONTACT_DETAILS language java parameter style java external name " +
            "'" + DerbyTestStoredProcedure.class.getName() + ".createContactDetails'";

    public static final String SQL_CREATE_SP_GET_MANAGER_DETAILS =
            "CREATE PROCEDURE getManagerDetails(IN NAME VARCHAR(32), OUT RESULT CONTACT_DETAILS)\n" +
            "PARAMETER STYLE JAVA\n" +
            "LANGUAGE JAVA\n" +
            "DYNAMIC RESULT SETS 0\n" +
            "EXTERNAL NAME '" + DerbyTestStoredProcedure.class.getName() + ".getManagerDetails'";

    public static final String CREATE_CONTACT_DETAILS_TYPE =
            "CREATE TYPE CONTACT_DETAILS EXTERNAL NAME '" + ContactDetails.class.getName() + "' LANGUAGE JAVA";

    @Override
    public void createPlanetTable(Connection connection) throws SQLException
    {
        executeDdl(connection, "CREATE TABLE PLANET(ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0)  NOT NULL PRIMARY KEY,POSITION INTEGER,NAME VARCHAR(255), DESCRIPTION CLOB)");
    }

    @Override
    protected String getInsertPlanetSql(String name, int position)
    {
        return "INSERT INTO PLANET(POSITION, NAME) VALUES (" + position + ", '" + name + "')";
    }

    @Override
    public void createStoredProcedureUpdateTestType1(DataSource dataSource) throws SQLException
    {
        createStoredProcedure(dataSource, SQL_CREATE_SP_UPDATE_TEST_TYPE_1);
    }

    @Override
    public void createStoredProcedureParameterizedUpdateTestType1(DataSource dataSource) throws SQLException
    {
        createStoredProcedure(dataSource, SQL_CREATE_SP_PARAM_UPDATE_TEST_TYPE_1);
    }

    @Override public void createStoredProcedureParameterizedUpdatePlanetDescription(DataSource dataSource) throws SQLException {
        createStoredProcedure(dataSource, SQL_CREATE_SP_PARAM_UPDATE_PLANET_DESCRIPTION);
    }

    @Override
    public void createStoredProcedureCountRecords(DataSource dataSource) throws SQLException
    {
        createStoredProcedure(dataSource, SQL_CREATE_SP_COUNT_RECORDS);
    }

    @Override
    public void createStoredProcedureGetRecords(DataSource dataSource) throws SQLException
    {
        createStoredProcedure(dataSource, SQL_CREATE_SP_GET_RECORDS);
    }

    @Override
    public void createStoredProcedureGetSplitRecords(DataSource dataSource) throws SQLException
    {
        createStoredProcedure(dataSource, SQL_CREATE_SP_GET_SPLIT_RECORDS);
    }

    @Override
    public void createStoredProcedureDoubleMyInt(DataSource dataSource) throws SQLException
    {
        createStoredProcedure(dataSource, SQL_CREATE_SP_DOUBLE_MY_INT);
    }

    @Override
    protected boolean supportsSimpleUdt()
    {
        return true;
    }

    @Override
    public void createStoredProcedureGetManagerDetails(DataSource dataSource) throws SQLException
    {
        createStoredProcedure(dataSource, SQL_CREATE_SP_GET_MANAGER_DETAILS);
    }

    @Override
    protected void createContactDetailsType(Connection connection) throws SQLException
    {
        try
        {
            String ddl = CREATE_CONTACT_DETAILS_TYPE;

            executeDdl(connection, ddl);
        }
        catch (SQLException e)
        {
            // Ignore exception when type already exists
            if (!DERBY_ERROR_OBJECT_ALREADY_EXISTS.equals(e.getSQLState()))
            {
                throw e;
            }
        }
    }

    @Override
    protected void deleteRegionManagersTable(Connection connection) throws SQLException
    {
        executeUpdate(connection, "DELETE FROM REGION_MANAGERS");
    }

    @Override
    protected void createRegionManagersTable(Connection connection) throws SQLException
    {
        String ddl = "CREATE TABLE REGION_MANAGERS(" +
                     "REGION_NAME VARCHAR(32) NOT NULL PRIMARY KEY," +
                     "MANAGER_NAME VARCHAR(32) NOT NULL," +
                     "DETAILS CONTACT_DETAILS NOT NULL)" ;
        executeDdl(connection, ddl);

        executeDdl(connection, SQL_CREATE_CONTACT_DETAIL_FUNCTION);
    }

    @Override
    protected String getInsertRegionManagerSql(RegionManager regionManager)
    {
        StringBuilder builder = new StringBuilder("INSERT INTO REGION_MANAGERS VALUES ('").append(regionManager.getRegionName()).append("', '")
                .append(regionManager.getName()).append("', createContactDetails('")
                .append(regionManager.getContactDetails().getDescription()).append("', '")
                .append(regionManager.getContactDetails().getPhoneNumber()).append("', '")
                .append(regionManager.getContactDetails().getEmail())
                .append("'))");

        return builder.toString();
    }

    @Override
    public void createStoredProcedureMultiplyInts(DataSource dataSource) throws SQLException
    {
        createStoredProcedure(dataSource, SQL_CREATE_SP_MULTIPLY_INTS);
    }

    @Override
    public void createStoredProcedureConcatenateStrings(DataSource dataSource) throws SQLException
    {
        createStoredProcedure(dataSource, SQL_CREATE_SP_CONCATENATE_STRINGS);
    }

    @Override
    public void createDelayFunction(DataSource dataSource) throws SQLException
    {
        createStoredProcedure(dataSource, SQL_CREATE_DELAY_FUNCTION);
    }

    public void createStoredProcedure(DataSource dataSource, String sql) throws SQLException
    {
        try
        {
            super.createStoredProcedure(dataSource, sql);
        }
        catch (SQLException e)
        {
            // Ignore exception when stored procedure already exists
            if (!DERBY_ERROR_OBJECT_ALREADY_EXISTS.equals(e.getSQLState()))
            {
                throw e;
            }
        }
    }
}
