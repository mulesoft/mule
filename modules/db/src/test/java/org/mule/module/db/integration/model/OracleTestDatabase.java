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

/**
 * Defines an Oracle test database to use with ojdbc7 driver.
 */
public class OracleTestDatabase extends AbstractTestDatabase
{

    @Override
    public void createPlanetTable(Connection connection) throws SQLException
    {
        executeDdl(connection, "CREATE TABLE PLANET(ID INTEGER NOT NULL PRIMARY KEY,POSITION SMALLINT, NAME VARCHAR(255), DESCRIPTION CLOB)");

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
    public void createFunctionGetRecords(DataSource dataSource) throws SQLException
    {
        String query = "CREATE OR REPLACE FUNCTION getTestRecordsFunction\n" +
                       "RETURN SYS_REFCURSOR\n" +
                       "IS planet_cursor SYS_REFCURSOR;\n" +
                       "BEGIN\n" +
                       "  OPEN planet_cursor FOR\n" +
                       "  SELECT * FROM planet;\n" +
                       "  RETURN planet_cursor;\n" +
                       "END;";

        executeDdl(dataSource, query);
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

  @Override public void createStoredProcedureParameterizedUpdatePlanetDescription(DataSource dataSource) throws SQLException {
    final String sql = "CREATE OR REPLACE PROCEDURE updatePlanetDescription (p_name IN VARCHAR2, p_description CLOB)\n" +
      "AS\n" +
      "BEGIN\n" +
      "  UPDATE PLANET SET DESCRIPTION=p_description WHERE name=p_name;\n" +
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
    public void createDelayFunction(DataSource dataSource) throws SQLException
    {

        final String sql = "CREATE OR REPLACE FUNCTION DELAY(seconds number) " +
                           "RETURN number IS " +
                           "targetDate DATE; " +
                           "BEGIN SELECT sysdate + seconds * 10/864 INTO targetDate FROM DUAL; " +
                           "LOOP EXIT WHEN SYSDATE >= targetDate; " +
                           "END LOOP; " +
                           "RETURN 1; " +
                           "END;";

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
        return DataType.DECIMAL;
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

    @Override
    protected boolean supportsSimpleUdt()
    {
        return true;
    }

    @Override
    protected boolean supportsArraysUdt()
    {
        return true;
    }

    @Override
    protected void createZipArrayType(Connection connection) throws SQLException
    {
        final String ddl = "CREATE OR REPLACE TYPE ZIPARRAY AS VARRAY(10) OF VARCHAR2(12)";
        executeDdl(connection, ddl);
    }

    @Override
    protected void createContactDetailsType(Connection connection) throws SQLException
    {
        final String ddl = "CREATE OR REPLACE TYPE CONTACT_DETAILS AS object(" +
                           "DESCRIPTION VARCHAR2(12)," +
                           "PHONE_NUMBER VARCHAR2(12)," +
                           "EMAIL_ADDRESS VARCHAR2(100))";

        try
        {
            executeDdl(connection, ddl);
        }
        catch (SQLException e)
        {
            // If the type already exists, ignore the error
            if (!e.getMessage().contains("ORA-02303"))
            {
                throw e;
            }
        }
    }

    @Override
    protected void createContactDetailsArrayType(Connection connection) throws SQLException
    {
        final String ddl = "CREATE OR REPLACE TYPE CONTACT_DETAILS_ARRAY AS VARRAY(100) OF CONTACT_DETAILS";
        executeDdl(connection, ddl);
    }

    @Override
    public void createStoredProcedureGetZipCodes(DataSource dataSource) throws SQLException
    {
        final String sql = "CREATE OR REPLACE PROCEDURE getZipCodes(pName IN VARCHAR2, pZipCodes OUT ZIPARRAY) " +
                           "IS " +
                           "BEGIN " +
                           "select ZIPS into pZipCodes from REGIONS where REGION_NAME = pName; " +
                           "END;";

        executeDdl(dataSource, sql);
    }

    @Override
    public void createStoredProcedureUpdateZipCodes(DataSource dataSource) throws SQLException
    {
        final String sql = "CREATE OR REPLACE PROCEDURE updateZipCodes(pName IN VARCHAR2, pZipCodes IN ZIPARRAY) " +
                           "IS " +
                           "BEGIN " +
                           "UPDATE REGIONS SET ZIPS = pZipCodes where REGION_NAME = pName; " +
                           "END;";

        executeDdl(dataSource, sql);
    }

    @Override
    public void createStoredProcedureUpdateContactDetails(DataSource dataSource) throws SQLException
    {
        final String sql = "CREATE OR REPLACE PROCEDURE updateContactDetails(pName IN VARCHAR2, pDetails IN CONTACT_DETAILS_ARRAY) " +
                           "IS " +
                           "BEGIN " +
                           "UPDATE CONTACTS SET DETAILS = pDetails where CONTACT_NAME= pName;" +
                           "END;";

        executeDdl(dataSource, sql);
    }

    @Override
    public void createStoredProcedureGetContactDetails(DataSource dataSource) throws SQLException
    {
        final String sql = "CREATE OR REPLACE PROCEDURE getContactDetails(pName IN VARCHAR2, pContactDetails OUT CONTACT_DETAILS_ARRAY) " +
                           "IS " +
                           "BEGIN " +
                           "select DETAILS into pContactDetails from CONTACTS where CONTACT_NAME= pName; " +
                           "END;";

        executeDdl(dataSource, sql);
    }

    @Override
    public void createStoredProcedureGetManagerDetails(DataSource dataSource) throws SQLException
    {
        final String sql = "CREATE OR REPLACE PROCEDURE getManagerDetails(pName IN VARCHAR2, pDetails OUT CONTACT_DETAILS) " +
                           "IS " +
                           "BEGIN " +
                           "select DETAILS into pDetails from REGION_MANAGERS where REGION_NAME= pName; " +
                           "END;";

        executeDdl(dataSource, sql);
    }

    @Override
    protected String getInsertContactSql(Contact contact)
    {
        StringBuilder builder = new StringBuilder("INSERT INTO CONTACTS VALUES ('").append(contact.getName()).append("', CONTACT_DETAILS_ARRAY(");

        boolean first = true;
        for (ContactDetails contactDetails : contact.getDetails())
        {
            if (first)
            {
                first = false;
            }
            else
            {
                builder.append(",");
            }
            builder.append("CONTACT_DETAILS('" ).append(contactDetails.getDescription()).append("', '").append(contactDetails.getPhoneNumber()).append("', '").append(contactDetails.getEmail()).append("')");
        }
        builder.append("))");

        return builder.toString();
    }

    @Override
    protected void createContactsTable(Connection connection) throws SQLException
    {
        String ddl = "create table CONTACTS " +
                     "(CONTACT_NAME varchar(32) NOT NULL," +
                     "DETAILS CONTACT_DETAILS_ARRAY NOT NULL," +
                     "PRIMARY KEY (CONTACT_NAME))";

        executeDdl(connection, ddl);
    }

    @Override
    protected void deleteContactsTable(Connection connection) throws SQLException
    {
        executeUpdate(connection, "DELETE FROM CONTACTS");
    }

    @Override
    protected String getInsertRegionSql(Region region)
    {
        StringBuilder builder = new StringBuilder("INSERT INTO REGIONS VALUES ('").append(region.getName()).append("', ").append(" ZIPARRAY(");

        boolean first = true;
        for (String zipCode : region.getZips())
        {
            if (first)
            {
                first = false;
            }
            else
            {
                builder.append(",");
            }
            builder.append(zipCode);
        }
        builder.append("))");

        return builder.toString();
    }

    @Override
    protected void createRegionsTable(Connection connection) throws SQLException
    {
        String ddl = "create table REGIONS " +
                     "(REGION_NAME varchar(32) NOT NULL," +
                     "ZIPS ZIPARRAY NOT NULL," +
                     "PRIMARY KEY (REGION_NAME))";

        executeDdl(connection, ddl);
    }

    @Override
    protected void deleteRegionsTable(Connection connection) throws SQLException
    {
        executeUpdate(connection, "DELETE FROM REGIONS");
    }

    @Override
    protected void createRegionManagersTable(Connection connection) throws SQLException
    {
        String ddl = "create table REGION_MANAGERS(" +
                     "REGION_NAME varchar(32) NOT NULL," +
                     "MANAGER_NAME varchar(32) NOT NULL," +
                     "DETAILS CONTACT_DETAILS NOT NULL," +
                     "PRIMARY KEY (REGION_NAME));" ;

        executeDdl(connection, ddl);
    }

    @Override
    protected void deleteRegionManagersTable(Connection connection) throws SQLException
    {
        executeUpdate(connection, "DELETE FROM REGION_MANAGERS");
    }

    @Override
    protected String getInsertRegionManagerSql(RegionManager regionManager)
    {
        StringBuilder builder = new StringBuilder("INSERT INTO REGION_MANAGERS VALUES ('").append(regionManager.getRegionName()).append("', '")
                .append(regionManager.getName()).append("', CONTACT_DETAILS('")
                .append(regionManager.getContactDetails().getDescription()).append("', '")
                .append(regionManager.getContactDetails().getPhoneNumber()).append("', '")
                .append(regionManager.getContactDetails().getEmail())
                .append("'))");

        return builder.toString();
    }
}
