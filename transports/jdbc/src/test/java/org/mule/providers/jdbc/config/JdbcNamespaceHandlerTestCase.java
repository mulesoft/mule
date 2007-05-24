/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jdbc.config;

import org.mule.providers.jdbc.JdbcConnector;
import org.mule.providers.jdbc.test.TestDataSource;
import org.mule.tck.FunctionalTestCase;

import javax.sql.DataSource;


/**
 * Tests the "jdbc" namespace.
 */
public class JdbcNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "jdbc-namespace-config.xml";
    }

    public void testWithDataSource() throws Exception
    {
        JdbcConnector c = (JdbcConnector) managementContext.getRegistry().lookupConnector("jdbcConnector1");
        assertNotNull(c);
        
        DataSource ds = c.getDataSource();
        assertNotNull(ds);
        assertEquals(TestDataSource.class, ds.getClass());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    public void testWithDataSourceViaJndi() throws Exception
    {
        JdbcConnector c = (JdbcConnector) managementContext.getRegistry().lookupConnector("jdbcConnector2");
        assertNotNull(c);
        
        DataSource ds = c.getDataSource();
        assertNotNull(ds);
        assertEquals(TestDataSource.class, ds.getClass());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
}
