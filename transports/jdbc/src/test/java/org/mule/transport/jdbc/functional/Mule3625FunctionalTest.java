/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test for MULE-3625, submitted by community member Guy Veraghtert
 */
public class Mule3625FunctionalTest extends AbstractServiceAndFlowTestCase
{
    public Mule3625FunctionalTest(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "jdbc-mule-3625-service.xml"},
            {ConfigVariant.FLOW, "jdbc-mule-3625-flow.xml"}
        });
    }      
    
    /**
     * Test registering transaction manager for non-XA rtansaction
     *
     * @throws Exception
     */
    @Test
    public void testNonXaTx() throws Exception
    {
        DataSource dataSource = (DataSource) muleContext.getRegistry().lookupObject("hsqldbDataSource");
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();

        // make sure other tests didnt leave anything behind
        statement.execute("DROP SCHEMA PUBLIC CASCADE");

        statement.executeUpdate("create table TABLE_A (value varchar(1))");
        statement.executeUpdate("create table TABLE_B (value varchar(1))");
        statement.executeUpdate("insert into TABLE_A(value) values('n')");
        Thread.sleep(10000); //TODO DZ: sleeps in tests are not ideal
        ResultSet resultSet = statement.executeQuery("select count(*) from TABLE_B where value='y'");
        assertTrue(resultSet.next());
        assertEquals(1, resultSet.getLong(1));

        // clean up for later tests
        statement.execute("DROP SCHEMA PUBLIC CASCADE");
        resultSet.close();
        connection.close();
    }
}
