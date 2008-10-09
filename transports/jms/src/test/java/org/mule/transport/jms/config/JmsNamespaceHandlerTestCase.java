/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.config;

import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.filter.Filter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transaction.XaTransactionFactory;
import org.mule.transport.jms.DefaultRedeliveryHandler;
import org.mule.transport.jms.DefaultRedeliveryHandlerFactory;
import org.mule.transport.jms.JmsConnector;
import org.mule.transport.jms.filters.JmsPropertyFilter;
import org.mule.transport.jms.filters.JmsSelectorFilter;
import org.mule.transport.jms.test.TestConnectionFactory;
import org.mule.transport.jms.test.TestRedeliveryHandler;

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
        JmsConnector c = (JmsConnector)muleContext.getRegistry().lookupConnector("jmsConnectorDefaults");
        assertNotNull(c);

        assertNotNull(c.getConnectionFactory());
        assertTrue(c.getConnectionFactory() instanceof TestConnectionFactory);
        assertEquals(Session.AUTO_ACKNOWLEDGE, c.getAcknowledgementMode());
        assertNull(c.getUsername());
        assertNull(c.getPassword());

        assertNotNull(c.getRedeliveryHandlerFactory());
        assertTrue(c.getRedeliveryHandlerFactory() instanceof DefaultRedeliveryHandlerFactory);
        assertTrue(c.getRedeliveryHandlerFactory().create() instanceof DefaultRedeliveryHandler);
        
        assertNull(c.getClientId());
        assertFalse(c.isDurable());
        assertFalse(c.isNoLocal());
        assertFalse(c.isPersistentDelivery());
        assertEquals(0, c.getMaxRedelivery());
        assertFalse(c.isCacheJmsSessions());
        assertTrue(c.isRecoverJmsConnections());
        assertTrue(c.isEagerConsumer());
        assertEquals(4, c.getNumberOfConcurrentTransactedReceivers());
    }
    
    public void testConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector1");
        assertNotNull(c);

        assertNotNull(c.getConnectionFactory());
        
        assertTrue(c.getConnectionFactory() instanceof TestConnectionFactory);
        assertEquals(Session.DUPS_OK_ACKNOWLEDGE, c.getAcknowledgementMode());
        assertEquals("myuser", c.getUsername());
        assertEquals("mypass", c.getPassword());

        assertNotNull(c.getRedeliveryHandlerFactory());
        assertTrue(c.getRedeliveryHandlerFactory().create() instanceof TestRedeliveryHandler);
        
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
        assertEquals(7, c.getNumberOfConcurrentTransactedReceivers());
    }

    public void testCustomConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector2");
        assertNotNull(c);

        assertEquals("1.1", c.getSpecification()); // 1.0.2b is the default, should be changed in the config
    }
    
    public void testTestConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector3");
        assertNotNull(c);

        assertNotNull(c.getConnectionFactory());

        assertTrue(c.getConnectionFactory() instanceof TestConnectionFactory);
        assertEquals(Session.DUPS_OK_ACKNOWLEDGE, c.getAcknowledgementMode());

        assertNotNull(c.getRedeliveryHandlerFactory());
        assertTrue(c.getRedeliveryHandlerFactory().create() instanceof TestRedeliveryHandler);

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
        ImmutableEndpoint endpoint1 = muleContext.getRegistry().lookupEndpointBuilder("endpoint1").buildInboundEndpoint();
        assertNotNull(endpoint1);
        Filter filter1 = endpoint1.getFilter();
        assertNotNull(filter1);
        assertTrue(filter1 instanceof JmsSelectorFilter);
        ImmutableEndpoint endpoint2 = muleContext.getRegistry().lookupEndpointBuilder("endpoint2").buildOutboundEndpoint();
        assertNotNull(endpoint2);
        Filter filter2 = endpoint2.getFilter();
        assertNotNull(filter2);
        assertTrue(filter2 instanceof NotFilter);
        Filter filter3 = ((NotFilter) filter2).getFilter();
        assertNotNull(filter3);
        assertTrue(filter3 instanceof JmsPropertyFilter);
    }

    public void testCustomTransactions() throws EndpointException, InitialisationException
    {
        ImmutableEndpoint endpoint3 = muleContext.getRegistry().lookupEndpointBuilder("endpoint3").buildInboundEndpoint();
        assertNotNull(endpoint3);
        TestTransactionFactory factory = (TestTransactionFactory) endpoint3.getTransactionConfig().getFactory();
        assertNotNull(factory);
        assertEquals("foo", factory.getValue());
    }
    
    public void testXaTransactions() throws Exception
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupEndpointBuilder("endpoint4").buildInboundEndpoint();
        assertNotNull(endpoint);
        assertEquals(XaTransactionFactory.class,
            endpoint.getTransactionConfig().getFactory().getClass());
        assertEquals(MuleTransactionConfig.ACTION_ALWAYS_JOIN, endpoint.getTransactionConfig().getAction());
    }

    public void testJndiConnectorAtributes() throws Exception
    {
        JmsConnector connector = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsJndiConnector");
        assertNotNull(connector);

        assertEquals("org.mule.transport.jms.test.JmsTestContextFactory", connector.getJndiInitialFactory());
        assertEquals("jndi://test", connector.getJndiProviderUrl());
        assertEquals("jms/connectionFactory", connector.getConnectionFactoryJndiName());
        assertEquals("org.mule.transport.jms.test.TestConnectionFactory", connector.getConnectionFactory().getClass().getName());
        assertTrue(connector.isJndiDestinations());
        assertTrue(connector.isForceJndiDestinations());
        assertEquals("value", connector.getJndiProviderProperties().get("key"));
    }
}
