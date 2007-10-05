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
import org.mule.providers.jms.test.TestConnectionFactory;
import org.mule.providers.jms.test.TestRedeliveryHandler;
import org.mule.tck.FunctionalTestCase;

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
        assertTrue(c.getConnectionFactory().create() instanceof TestConnectionFactory);
        assertEquals(Session.AUTO_ACKNOWLEDGE, c.getAcknowledgementMode());
        assertNull(c.getUsername());
        assertNull(c.getPassword());

        assertNotNull(c.getRedeliveryHandler());
        assertTrue(c.getRedeliveryHandler().create() instanceof DefaultRedeliveryHandler);
        
        assertNull(c.getClientId());
        assertFalse(c.isDurable());
        assertFalse(c.isNoLocal());
        assertFalse(c.isPersistentDelivery());
        assertEquals(0, c.getMaxRedelivery());
        assertFalse(c.isCacheJmsSessions());
        assertTrue(c.isRecoverJmsConnections());
        assertTrue(c.isEagerConsumer());
        assertFalse(c.isJndiDestinations());
        assertFalse(c.isForceJndiDestinations());
    }
    
    public void testConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) managementContext.getRegistry().lookupConnector("jmsConnector1");
        assertNotNull(c);

        assertNotNull(c.getConnectionFactory());
        
        assertTrue(c.getConnectionFactory().create() instanceof TestConnectionFactory);
        assertEquals(Session.DUPS_OK_ACKNOWLEDGE, c.getAcknowledgementMode());
        assertEquals("myuser", c.getUsername());
        assertEquals("mypass", c.getPassword());

        assertNotNull(c.getRedeliveryHandler());
        assertTrue(c.getRedeliveryHandler().create() instanceof TestRedeliveryHandler);
        
        assertEquals("myClient", c.getClientId());
        assertTrue(c.isDurable());
        assertTrue(c.isNoLocal());
        assertTrue(c.isPersistentDelivery());
        assertEquals(5, c.getMaxRedelivery());
        assertTrue(c.isCacheJmsSessions());
        assertFalse(c.isRecoverJmsConnections());
        assertFalse(c.isEagerConsumer());
        assertTrue(c.isJndiDestinations());
        assertTrue(c.isForceJndiDestinations());

        assertEquals("1.1", c.getSpecification()); // 1.0.2b is the default, should be changed in the config
    }

    public void testCustomConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) managementContext.getRegistry().lookupConnector("jmsConnector2");
        assertNotNull(c);

        assertEquals("1.1", c.getSpecification()); // 1.0.2b is the default, should be changed in the config
    }
    
//    public void testJndi() throws Exception
//    {
//        JmsConnector c = (JmsConnector)managementContext.getRegistry().lookupConnector("jmsConnector2");
//        assertNotNull(c);
//
//        assertNotNull(c.getConnectionFactory());
//        assertTrue(c.getConnectionFactory().create() instanceof TestConnectionFactory);
//        assertEquals(Session.DUPS_OK_ACKNOWLEDGE, c.getAcknowledgementMode());
//    }
}
