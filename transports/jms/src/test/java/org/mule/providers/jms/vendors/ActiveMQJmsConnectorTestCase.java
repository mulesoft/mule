/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.vendors;

import org.mule.providers.jms.DefaultJmsTopicResolver;
import org.mule.providers.jms.DefaultRedeliveryHandler;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.activemq.ActiveMQJmsConnector;
import org.mule.providers.jms.test.TestRedeliveryHandler;
import org.mule.tck.FunctionalTestCase;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;

public class ActiveMQJmsConnectorTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "activemq-config.xml";
    }

    public void testConfigurationDefaults() throws Exception
    {
        JmsConnector c = (JmsConnector)managementContext.getRegistry().lookupConnector("jmsConnector");
        assertNotNull(c);

        assertFalse(c.isEagerConsumer());
        
        ConnectionFactory cf = (ConnectionFactory) c.getConnectionFactory().getOrCreate();
        assertTrue(cf instanceof ActiveMQConnectionFactory);
        assertEquals(ActiveMQJmsConnector.BROKER_URL, ((ActiveMQConnectionFactory) cf).getBrokerURL());
        
        assertNotNull(c.getTopicResolver());
        assertTrue("Wrong topic resolver configured on the connector.",
                   c.getTopicResolver() instanceof DefaultJmsTopicResolver);
    }
    
    public void testDefaultActiveMqConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) managementContext.getRegistry().lookupConnector("activeMqJmsConnector");

        assertNotNull(c);
        assertTrue(c instanceof ActiveMQJmsConnector);
        
        assertNotNull(c.getConnectionFactory());
        assertTrue(c.getConnectionFactory().getOrCreate() instanceof ActiveMQConnectionFactory);
        assertEquals(Session.AUTO_ACKNOWLEDGE, c.getAcknowledgementMode());
        assertNull(c.getUsername());
        assertNull(c.getPassword());

        assertNotNull(c.getRedeliveryHandler());
        assertTrue(c.getRedeliveryHandler().getOrCreate() instanceof DefaultRedeliveryHandler);
        
        assertFalse(c.isDurable());
        assertFalse(c.isNoLocal());
        assertFalse(c.isPersistentDelivery());
        assertEquals(0, c.getMaxRedelivery());
        assertFalse(c.isCacheJmsSessions());
        assertTrue(c.isRecoverJmsConnections());
        assertFalse(c.isEagerConsumer());
        assertFalse(c.isJndiDestinations());
        assertFalse(c.isForceJndiDestinations());

        assertEquals("1.0.2b", c.getSpecification());
    }
    
    public void testCustomActiveMqConnectorConfig() throws Exception
    {
        JmsConnector c = (JmsConnector) managementContext.getRegistry().lookupConnector("customActiveMqJmsConnector");

        assertNotNull(c);
        assertTrue(c instanceof ActiveMQJmsConnector);
        
        assertNotNull(c.getConnectionFactory());
        assertTrue(c.getConnectionFactory().getOrCreate() instanceof ActiveMQConnectionFactory);
        assertEquals(Session.DUPS_OK_ACKNOWLEDGE, c.getAcknowledgementMode());
        assertNull(c.getUsername());
        assertNull(c.getPassword());

        assertNotNull(c.getRedeliveryHandler());
        assertTrue(c.getRedeliveryHandler().getOrCreate() instanceof TestRedeliveryHandler);

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
}