/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.model.derbyutil;

import org.mule.module.db.integration.model.ContactDetails;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Defines stored procedures for testing purposes. Used by reflection from {@link org.mule.module.db.integration.model.DerbyTestDatabase}
 */
@SuppressWarnings("UnusedDeclaration")
public class DerbyTestStoredProcedure
{

    public static void selectRows(ResultSet[] data1) throws SQLException
    {

        Connection conn = DriverManager.getConnection("jdbc:default:connection");
        try
        {
            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM PLANET");
            data1[0] = ps1.executeQuery();
        }
        finally
        {
            conn.close();
        }
    }

    public static void updateTestType1() throws SQLException
    {
        Connection conn = DriverManager.getConnection("jdbc:default:connection");

        try
        {
            Statement ps1 = conn.createStatement();
            ps1.executeUpdate("UPDATE PLANET SET NAME='Mercury' WHERE POSITION=4");
        }
        finally
        {
            conn.close();
        }
    }

    public static void updateParameterizedTestType1(String name) throws SQLException
    {
        Connection conn = DriverManager.getConnection("jdbc:default:connection");

        try
        {
            Statement ps1 = conn.createStatement();
            ps1.executeUpdate("UPDATE PLANET SET NAME='" + name + "' WHERE POSITION=4");
        }
        finally
        {
            conn.close();
        }
    }

    public static void updatePlanetDescription(String name, Clob description) throws SQLException
    {
        Connection conn = DriverManager.getConnection("jdbc:default:connection");

        try
        {
            PreparedStatement ps1 = conn.prepareStatement("UPDATE PLANET SET DESCRIPTION=? WHERE NAME=?");
            ps1.setClob(1, description);
            ps1.setString(2, name);
            ps1.execute();
        }
        finally
        {
            conn.close();
        }
    }

    public static void countTestRecords(int[] count) throws SQLException
    {
        Connection conn = DriverManager.getConnection("jdbc:default:connection");

        try
        {
            Statement ps1 = conn.createStatement();
            ResultSet resultSet = ps1.executeQuery("SELECT COUNT(*) FROM PLANET");
            resultSet.next();
            count[0] = resultSet.getInt(1);
        }
        finally
        {
            conn.close();
        }
    }

    public static void getTestRecords(ResultSet[] data1) throws SQLException
    {
        Connection conn = DriverManager.getConnection("jdbc:default:connection");

        try
        {
            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM PLANET");
            data1[0] = ps1.executeQuery();
        }
        finally
        {
            conn.close();
        }
    }

    public static void getSplitTestRecords(ResultSet[] data1, ResultSet[] data2) throws SQLException
    {
        Connection conn = DriverManager.getConnection("jdbc:default:connection");

        try
        {
            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM PLANET WHERE POSITION <= 2");
            data1[0] = ps1.executeQuery();

            PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM PLANET WHERE POSITION > 2");
            data2[0] = ps2.executeQuery();
        }
        finally
        {
            conn.close();
        }
    }

    public static void doubleMyInt(int[] i)
    {
        i[0] *= 2;
    }

    public static void multiplyInts(int int1, int int2, int[] result1, int int3, int[] result2)
    {
        result1[0] = int1 * int2;
        result2[0] = int1 * int2 * int3;
    }

    public static void concatenateStrings(String string1, String string2, String[] result)
    {
        result[0] = string1 + string2;
    }

    public static int timeDelay(int secondsDelay)
    {
        if (secondsDelay > 0)
        {
            try
            {
                Thread.sleep(secondsDelay * 1000);
            }
            catch (InterruptedException e)
            {
                return 0;
            }
        }

        return 1;
    }

    public static ContactDetails createContactDetails(String description, String phone, String email)
    {
        return new ContactDetails(description, phone, email);
    }

    public static void getManagerDetails(String name, ContactDetails[] result) throws SQLException
    {
        Connection conn = DriverManager.getConnection("jdbc:default:connection");

        try
        {
            Statement ps1 = conn.createStatement();
            ResultSet resultSet = ps1.executeQuery("SELECT DETAILS FROM REGION_MANAGERS WHERE REGION_NAME ='" + name + "'");
            resultSet.next();
            result[0] = (ContactDetails) resultSet.getObject(1);
        }
        finally
        {
            conn.close();
        }
    }

}
