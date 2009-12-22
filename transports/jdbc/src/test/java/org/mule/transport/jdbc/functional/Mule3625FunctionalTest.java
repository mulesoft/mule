/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.functional;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.mule.tck.FunctionalTestCase;

/**
 * Test for MULE-3625, submitted by community member Guy Veraghtert
 */
public class Mule3625FunctionalTest extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "jdbc-mule-3625.xml";
    }

    /**
     * Test registering transaction manager for non-XA rtansaction
     * 
     * @throws Exception
     */
    public void testNonXaTx() throws Exception
    {
        DataSource dataSource = (DataSource) muleContext.getRegistry().lookupObject("hsqldbDataSource");
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        statement.executeUpdate("create table TABLE_A (value varchar(1))");
        statement.executeUpdate("create table TABLE_B (value varchar(1))");
        statement.executeUpdate("insert into TABLE_A(value) values('n')");
        Thread.sleep(10000); //TODO DZ: sleeps in tests are not ideal
        ResultSet resultSet = statement.executeQuery("select count(*) from TABLE_B where value='y'");
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.getLong(1));
    }

}
