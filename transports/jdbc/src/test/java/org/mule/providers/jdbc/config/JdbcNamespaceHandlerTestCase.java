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
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.util.object.ObjectFactory;
import org.mule.util.properties.PropertyExtractor;


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
        
        ObjectFactory dsf = c.getDataSourceFactory();
        assertNotNull(dsf);
        Object ds=dsf.create();
        assertNotNull(ds);
        assertEquals(TestDataSource.class, ds.getClass());
        
        assertTrue(c.getPropertyExtractors().size() >= 5);
        assertTrue(c.getPropertyExtractors().size() <= 7);
        assertTrue(ObjectFactory.class.isAssignableFrom((c.getPropertyExtractors().toArray()[0]).getClass()));
        assertTrue(((ObjectFactory)c.getPropertyExtractors().toArray()[0]).create() instanceof PropertyExtractor);
        assertNull(c.getQueries());
        
    }

    public void testWithDataSourceViaJndi() throws Exception
    {
        JdbcConnector c = (JdbcConnector) managementContext.getRegistry().lookupConnector("jdbcConnector2");
        assertNotNull(c);
        
        ObjectFactory dsf = c.getDataSourceFactory();
        assertNotNull(dsf);
        Object ds=dsf.create();
        assertNotNull(ds);
        assertEquals(TestDataSource.class, ds.getClass());
        
        assertTrue(c.getPropertyExtractors().size()>=5);
        assertTrue(c.getPropertyExtractors().size()<=7);
        assertTrue(ObjectFactory.class.isAssignableFrom((c.getPropertyExtractors().toArray()[0]).getClass()));
        assertTrue(((ObjectFactory)c.getPropertyExtractors().toArray()[0]).create() instanceof PropertyExtractor);
        assertNull(c.getQueries());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
    
    public void testFullyConfigured() throws Exception
    {
        JdbcConnector c = (JdbcConnector) managementContext.getRegistry().lookupConnector("jdbcConnector3");
        assertNotNull(c);
        
        ObjectFactory dsf = c.getDataSourceFactory();
        assertNotNull(dsf);
        Object ds=dsf.create();
        assertNotNull(ds);
        assertEquals(TestDataSource.class, ds.getClass());
        
        assertEquals(2,c.getPropertyExtractors().size());
        assertTrue(c.getPropertyExtractors().iterator().next() instanceof PropertyExtractor);
        assertNotNull(c.getQueries());
        assertEquals(3, c.getQueries().size());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
    
    
    public void testEndpointQueryOverride() throws Exception
    {
        JdbcConnector c = (JdbcConnector) managementContext.getRegistry().lookupConnector("jdbcConnector3");
        UMOEndpoint testJdbcEndpoint =  managementContext.getRegistry().lookupEndpoint("testJdbcEndpoint");
        
        //On connector, not overridden
        assertNotNull(c.getQuery(testJdbcEndpoint, "getTest"));
        
        //On connector, overridden on endpoint
        assertNotNull(c.getQuery(testJdbcEndpoint, "getTest2"));
        assertEquals("OVERRIDDEN VALUE",c.getQuery(testJdbcEndpoint, "getTest2"));
        
        //Only on endpoint
        assertNotNull(c.getQuery(testJdbcEndpoint, "getTest3"));

        //Does not exist on either
        assertNull(c.getQuery(testJdbcEndpoint, "getTest4"));

        
    }
}
