/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms.config;

import org.mule.providers.jms.DefaultRedeliveryHandler;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.filters.JmsPropertyFilter;
import org.mule.providers.jms.filters.JmsSelectorFilter;
import org.mule.providers.jms.test.TestConnectionFactory;
import org.mule.providers.jms.test.TestRedeliveryHandler;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.umo.UMOFilter;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;

import javax.jms.Session;


/**
 * Tests the "jms" namespace.
 */
public class JmsNamespaceHandlerTestCase extends FunctionalTestCase
{
    public JmsNamespaceHandlerTestCase()
    {
        setStartContext(false);
    }
    
    protected String getConfigResources()
    {
        return "jms-namespace-config.xml";
    }

    public void testDefaultConfig() throws Exception
    {
        JmsConnector c = (JmsConnector)managementContext.getRegistry().lookupConnector("jmsConnectorDefaults");
        assertNotNull(c);

        assertNotNull(c.getConnectionFactory());
        assertTrue(c.getConnectionFactory() instanceof TestConnectionFactory);
        assertEquals(Session.AUTO_ACKNOWLEDGE, c.getAcknowledgementMode());
        assertNull(c.getUsername());
        assertNull(c.getPassword());

        assertNotNull(c.getRedeliveryHandler());
        assertTrue(c.getRedeliveryHandler() instanceof DefaultRedeliveryHandler);
        
        assertNull(c.getClientId());
        assertFalse(c.isDurable());
        assertFalse(c.isNoLocal());
        assertFalse(c.isPersistentDelivery());
        assertEquals(0, c.getMaxRedelivery());
        assertFalse(c.isCacheJmsSessions());
        assertTrue(c.isRecoverJmsConnections());
        assertTrue(c.isEagerConsumer());
    }
    
    public void testConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) managementContext.getRegistry().lookupConnector("jmsConnector1");
        assertNotNull(c);

        assertNotNull(c.getConnectionFactory());
        
        assertTrue(c.getConnectionFactory() instanceof TestConnectionFactory);
        assertEquals(Session.DUPS_OK_ACKNOWLEDGE, c.getAcknowledgementMode());
        assertEquals("myuser", c.getUsername());
        assertEquals("mypass", c.getPassword());

        assertNotNull(c.getRedeliveryHandler());
        assertTrue(c.getRedeliveryHandler() instanceof TestRedeliveryHandler);
        
        assertEquals("myClient", c.getClientId());
        assertTrue(c.isDurable());
        assertTrue(c.isNoLocal());
        assertTrue(c.isPersistentDelivery());
        assertEquals(5, c.getMaxRedelivery());
        assertTrue(c.isCacheJmsSessions());
        assertFalse(c.isRecoverJmsConnections());
        assertFalse(c.isEagerConsumer());

        assertEquals("1.1", c.getSpecification()); // 1.0.2b is the default, should be changed in the config
        //test properties, default is 4
        assertEquals(c.getNumberOfConcurrentTransactedReceivers(),7);


    }

    public void testCustomConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) managementContext.getRegistry().lookupConnector("jmsConnector2");
        assertNotNull(c);

        assertEquals("1.1", c.getSpecification()); // 1.0.2b is the default, should be changed in the config
    }
    
    public void testTestConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) managementContext.getRegistry().lookupConnector("jmsConnector3");
        assertNotNull(c);

        assertNotNull(c.getConnectionFactory());

        assertTrue(c.getConnectionFactory() instanceof TestConnectionFactory);
        assertEquals(Session.DUPS_OK_ACKNOWLEDGE, c.getAcknowledgementMode());

        assertNotNull(c.getRedeliveryHandler());
        assertTrue(c.getRedeliveryHandler() instanceof TestRedeliveryHandler);

        assertEquals("myClient", c.getClientId());
        assertTrue(c.isDurable());
        assertTrue(c.isNoLocal());
        assertTrue(c.isPersistentDelivery());
        assertEquals(5, c.getMaxRedelivery());
        assertTrue(c.isCacheJmsSessions());
        assertFalse(c.isRecoverJmsConnections());
        assertFalse(c.isEagerConsumer());

        assertEquals("1.1", c.getSpecification()); // 1.0.2b is the default, should be changed in the config
    }

    public void testEndpointConfig() throws EndpointException, InitialisationException
    {
        UMOImmutableEndpoint endpoint1 = managementContext.getRegistry().lookupEndpointBuilder("endpoint1").buildInboundEndpoint();
        assertNotNull(endpoint1);
        UMOFilter filter1 = endpoint1.getFilter();
        assertNotNull(filter1);
        assertTrue(filter1 instanceof JmsSelectorFilter);
        UMOImmutableEndpoint endpoint2 = managementContext.getRegistry().lookupEndpointBuilder("endpoint2").buildOutboundEndpoint();
        assertNotNull(endpoint2);
        UMOFilter filter2 = endpoint2.getFilter();
        assertNotNull(filter2);
        assertTrue(filter2 instanceof NotFilter);
        UMOFilter filter3 = ((NotFilter) filter2).getFilter();
        assertNotNull(filter3);
        assertTrue(filter3 instanceof JmsPropertyFilter);
    }

    public void testCustomTransactions() throws EndpointException, InitialisationException
    {
        UMOImmutableEndpoint endpoint3 = managementContext.getRegistry().lookupEndpointBuilder("endpoint3").buildInboundEndpoint();
        assertNotNull(endpoint3);
        TestTransactionFactory factory = (TestTransactionFactory) endpoint3.getTransactionConfig().getFactory();
        assertNotNull(factory);
        assertEquals("foo", factory.getValue());
    }

}
