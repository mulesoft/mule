/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.config;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JdbcNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
