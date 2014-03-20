/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public class OracleTestDatabase extends AbstractTestDatabase
{

    @Override
    public void createDefaultTestTable(Connection connection) throws SQLException
    {
        executeDdl(connection, "CREATE TABLE PLANET(ID INTEGER NOT NULL PRIMARY KEY,POSITION SMALLINT, NAME VARCHAR(255))");

        executeDdl(connection, "CREATE SEQUENCE PLANET_SEQ INCREMENT BY 1 START WITH 1");

        executeDdl(connection,
                   "CREATE TRIGGER PLANET_TRIGGER\n" +
                   "BEFORE INSERT ON PLANET\n" +
                   "FOR EACH ROW WHEN (new.ID is null)\n" +
                   "begin\n" +
                   "    select PLANET_SEQ.nextval into :new.ID from dual;\n" +
                   "end;");
    }

    @Override
    protected String getInsertPlanetSql(String name, int position)
    {
        return "INSERT INTO PLANET(ID, POSITION, NAME) VALUES (PLANET_SEQ.NEXTVAL, " + position + ", '" + name + "')";
    }

    @Override
    public void createStoredProcedureGetRecords(DataSource dataSource) throws SQLException
    {
        throw new UnsupportedOperationException("Oracle only return values using output parameters");
    }

    @Override
    public void createStoredProcedureUpdateTestType1(DataSource dataSource) throws SQLException
    {
        throw new UnsupportedOperationException("Oracle only return values using output parameters");
    }

    @Override
    public void createStoredProcedureParameterizedUpdateTestType1(DataSource dataSource)
    {
        throw new UnsupportedOperationException("Oracle only return values using output parameters");
    }

    @Override
    public void createStoredProcedureCountRecords(DataSource dataSource) throws SQLException
    {
        final String sql = "CREATE OR REPLACE PROCEDURE countTestRecords(count OUT NUMBER) IS\n" +
                           "BEGIN\n" +
                           "   SELECT COUNT(*)\n" +
                           "   INTO   count\n" +
                           "   FROM   PLANET;\n" +
                           "END countTestRecords;";

        createStoredProcedure(dataSource, sql);
    }

    @Override
    public void createStoredProcedureGetSplitRecords(DataSource dataSource)
    {
        throw new UnsupportedOperationException("Oracle only return values using output parameters");
    }

    @Override
    public void createStoredProcedureDoubleMyInt(DataSource dataSource) throws SQLException
    {
        final String sql = "CREATE OR REPLACE PROCEDURE doubleMyInt(MYINT IN OUT NUMBER) IS\n" +
                           "BEGIN\n" +
                           "    SELECT MYINT * 2 \n" +
                           "    INTO   MYINT\n" +
                           "    FROM   DUAL;\n" +
                           "END doubleMyInt;";

        createStoredProcedure(dataSource, sql);
    }

    @Override
    public void createStoredProcedureMultiplyInts(DataSource dataSource) throws SQLException
    {
        final String sql = "CREATE OR REPLACE PROCEDURE multiplyInts(INT1 IN NUMBER, INT2 IN NUMBER, RESULT OUT NUMBER) IS\n" +
                           "BEGIN\n" +
                           "    SELECT INT1 * INT2 \n" +
                           "    INTO   RESULT\n" +
                           "    FROM   DUAL;\n" +
                           "END multiplyInts;";

        createStoredProcedure(dataSource, sql);
    }

    @Override
    public void createStoredProcedureConcatenateStrings(DataSource dataSource) throws SQLException
    {
        final String sql = "CREATE OR REPLACE PROCEDURE concatenateStrings(STRING1 IN VARCHAR2, STRING2 IN VARCHAR2, RESULT OUT VARCHAR2) IS\n" +
                           "BEGIN\n" +
                           "    SELECT STRING1 || STRING2\n" +
                           "    INTO   RESULT\n" +
                           "    FROM   DUAL;\n" +
                           "END concatenateStrings;";

        createStoredProcedure(dataSource, sql);
    }
}
