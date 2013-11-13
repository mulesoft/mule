/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transaction.XaTransactionFactory;
import org.mule.transport.jdbc.ExtendedQueryRunner;
import org.mule.transport.jdbc.JdbcConnector;
import org.mule.transport.jdbc.JdbcTransactionFactory;
import org.mule.transport.jdbc.sqlstrategy.DefaultSqlStatementStrategyFactory;
import org.mule.transport.jdbc.test.TestDataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.junit.Test;

public class JdbcNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "jdbc-namespace-config.xml";
    }

    @Test
    public void testWithDataSource() throws Exception
    {
        JdbcConnector c = (JdbcConnector) muleContext.getRegistry().lookupConnector("jdbcConnector1");
        assertNotNull(c);

        assertTrue(c.getDataSource() instanceof TestDataSource);
        assertNull(c.getQueries());
        assertEquals(-1, c.getQueryTimeout());
    }

    @Test
    public void testWithDataSourceViaJndi() throws Exception
    {
        JdbcConnector c = (JdbcConnector) muleContext.getRegistry().lookupConnector("jdbcConnector2");
        assertNotNull(c);

        assertTrue(c.getDataSource() instanceof TestDataSource);
        assertNull(c.getQueries());
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
        assertEquals(3, c.getQueryTimeout());
    }

    @Test
    public void testFullyConfigured() throws Exception
    {
        JdbcConnector c = (JdbcConnector) muleContext.getRegistry().lookupConnector("jdbcConnector3");
        assertNotNull(c);

        assertTrue(c.getDataSource() instanceof TestDataSource);

        assertNotNull(c.getQueries());
        assertEquals(3, c.getQueries().size());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
        // Test a abstract connector property (MULE-5776)
        assertTrue(c.isValidateConnections());
    }

    @Test
    public void testEndpointQueryOverride() throws Exception
    {
        JdbcConnector c = (JdbcConnector) muleContext.getRegistry().lookupConnector("jdbcConnector3");
        ImmutableEndpoint testJdbcEndpoint = muleContext.getEndpointFactory()
            .getInboundEndpoint("testJdbcEndpoint");

        //On connector, not overridden
        assertNotNull(c.getQuery(testJdbcEndpoint, "getTest"));

        //On connector, overridden on endpoint
        assertNotNull(c.getQuery(testJdbcEndpoint, "getTest2"));
        assertEquals("OVERRIDDEN VALUE", c.getQuery(testJdbcEndpoint, "getTest2"));

        //Only on endpoint
        assertNotNull(c.getQuery(testJdbcEndpoint, "getTest3"));

        //Does not exist on either
        assertNull(c.getQuery(testJdbcEndpoint, "getTest4"));
        assertEquals("3", testJdbcEndpoint.getProperty("queryTimeout"));

        QueryRunner queryRunner = c.getQueryRunnerFor(testJdbcEndpoint);
        assertEquals(ExtendedQueryRunner.class, queryRunner.getClass());
        assertEquals(3, ((ExtendedQueryRunner) queryRunner).getQueryTimeout());
    }

    @Test
    public void testEndpointWithTransaction() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().
            lookupEndpointBuilder("endpointWithTransaction").buildInboundEndpoint();
        assertNotNull(endpoint);
        assertEquals(JdbcTransactionFactory.class,
            endpoint.getTransactionConfig().getFactory().getClass());
        assertEquals(MuleTransactionConfig.ACTION_NONE,
            endpoint.getTransactionConfig().getAction());
        assertEquals("-1", endpoint.getProperty("queryTimeout"));
    }

    @Test
    public void testEndpointWithXaTransaction() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().
            lookupEndpointBuilder("endpointWithXaTransaction").buildInboundEndpoint();
        assertNotNull(endpoint);
        assertEquals(XaTransactionFactory.class,
            endpoint.getTransactionConfig().getFactory().getClass());
        assertEquals(MuleTransactionConfig.ACTION_ALWAYS_BEGIN,
            endpoint.getTransactionConfig().getAction());
    }

    @Test
    public void testSqlStatementStrategyFactoryOverride() throws Exception
    {
        // class config
        JdbcConnector c = (JdbcConnector) muleContext.getRegistry().lookupConnector("jdbcConnector4");
        assertNotNull(c.getSqlStatementStrategyFactory());
        assertTrue(c.getSqlStatementStrategyFactory() instanceof TestSqlStatementStrategyFactory);

        // ref config
        c = (JdbcConnector) muleContext.getRegistry().lookupConnector("jdbcConnector5");
        assertNotNull(c.getSqlStatementStrategyFactory());
        assertTrue(c.getSqlStatementStrategyFactory() instanceof TestSqlStatementStrategyFactory);
    }

    public static class TestSqlStatementStrategyFactory extends DefaultSqlStatementStrategyFactory
    {
        // no custom methods
    }
}
