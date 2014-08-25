/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model;

import org.mule.common.metadata.datatype.DataType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public class OracleTestDatabase extends AbstractTestDatabase
{

    @Override
    public void createPlanetTable(Connection connection) throws SQLException
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

        final String sql = "CREATE OR REPLACE PROCEDURE getTestRecords ( st_cursor OUT SYS_REFCURSOR  )\n" +
                           "     is\n" +
                           " BEGIN\n" +
                           "  OPEN st_cursor FOR\n" +
                           "  SELECT * FROM PLANET;\n" +
                           " end;\n";

        createStoredProcedure(dataSource, sql);
    }

    @Override
    public void createStoredProcedureUpdateTestType1(DataSource dataSource) throws SQLException
    {
        final String sql = "CREATE OR REPLACE PROCEDURE updateTestType1 (p_retVal OUT INTEGER)\n" +
                           "AS\n" +
                           "BEGIN\n" +
                           "  UPDATE PLANET SET NAME='Mercury' WHERE POSITION=4;\n" +
                           "   p_retVal := SQL%ROWCOUNT;\n" +
                           "END;";

        createStoredProcedure(dataSource, sql);
    }

    @Override
    public void createStoredProcedureParameterizedUpdateTestType1(DataSource dataSource) throws SQLException
    {
        final String sql = "CREATE OR REPLACE PROCEDURE updateParamTestType1 (p_name IN VARCHAR2, p_retVal OUT INTEGER)\n" +
                           "AS\n" +
                           "BEGIN\n" +
                           "  UPDATE PLANET SET NAME=p_Name WHERE POSITION=4;\n" +
                           "\n" +
                           "   p_retVal := SQL%ROWCOUNT;\n" +
                           "END;";

        createStoredProcedure(dataSource, sql);
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
    public void createStoredProcedureGetSplitRecords(DataSource dataSource) throws SQLException
    {
        final String sql = "CREATE OR REPLACE PROCEDURE getSplitTestRecords ( st_cursor1 OUT SYS_REFCURSOR, st_cursor2 OUT SYS_REFCURSOR  )\n" +
                           "is\n" +
                           "BEGIN\n" +
                           "   OPEN st_cursor1 FOR SELECT * FROM PLANET WHERE POSITION <= 2;\n" +
                           "   OPEN st_cursor2 FOR SELECT * FROM PLANET WHERE POSITION > 2;\n" +
                           "END;";

        createStoredProcedure(dataSource, sql);
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
        final String sql = "CREATE OR REPLACE PROCEDURE multiplyInts(INT1 IN NUMBER, INT2 IN NUMBER, RESULT1 OUT NUMBER, INT3 IN NUMBER, RESULT2 OUT NUMBER) IS\n" +
                           "BEGIN\n" +
                           "    SELECT INT1 * INT2 \n" +
                           "    INTO   RESULT1\n" +
                           "    FROM   DUAL;\n" +
                           "    SELECT INT1 * INT2 * INT3 \n" +
                           "    INTO   RESULT2\n" +
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

    @Override
    public Class getIdFieldJavaClass()
    {
        return BigDecimal.class;
    }

    @Override
    public Class getDefaultAutoGeneratedKeyClass()
    {
        try
        {
            return Class.forName("oracle.sql.ROWID");
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalStateException("Unable to load ROWDID class");
        }
    }

    @Override
    public DataType getIdFieldInputMetaDataType()
    {
        return DataType.STRING;
    }

    @Override
    public DataType getIdFieldOutputMetaDataType()
    {
        return DataType.DECIMAL;
    }

    @Override
    public DataType getPositionFieldOutputMetaDataType()
    {
        return DataType.DECIMAL;
    }

    @Override
    protected void createAlienTable(Connection connection) throws SQLException
    {
        String ddl = "CREATE TABLE ALIEN(\n" +
                     "  NAME varchar2(255),\n" +
                     "  DESCRIPTION XMLTYPE)";
        executeDdl(connection, ddl);
    }

    @Override
    protected String getInsertAlienSql(Alien alien)
    {
        String sql = "INSERT INTO Alien VALUES ('" + alien.getName()  + "' , XMLType('" +
                     alien.getXml()+ "'))";

        return sql;
    }

    @Override
    public void createStoredProcedureGetAlienDescription(DataSource dataSource) throws SQLException
    {
        final String sql = "CREATE OR REPLACE PROCEDURE getAlienDescription(pName IN VARCHAR2, pDescription OUT XMLType)\n" +
                     "IS\n" +
                     "BEGIN\n" +
                     "    select description into pDescription from Alien where name= pName; \n" +
                     "END;\n";

        executeDdl(dataSource, sql);
    }

    @Override
    protected boolean supportsXmlType()
    {
        return true;
    }

    @Override
    public void createStoredProcedureUpdateAlienDescription(DataSource dataSource) throws SQLException
    {
        final String sql = "CREATE OR REPLACE PROCEDURE updateAlienDescription(pName IN VARCHAR2, pDescription in XMLType)\n" +
                           "IS\n" +
                           "BEGIN\n" +
                           "    update Alien set description = pDescription where name= pName; \n" +
                           "END;\n";

        executeDdl(dataSource, sql);
    }
}
